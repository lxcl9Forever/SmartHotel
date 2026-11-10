/**
 * 1000 distinct test customers competing for one daily deal.
 * Run only against a disposable, isolated SmartHotel deployment.
 * Node 18+; no third-party packages required. No payment API is called.
 */
import http from 'node:http'
import https from 'node:https'
import { randomBytes } from 'node:crypto'
import { mkdir, writeFile } from 'node:fs/promises'
import { dirname, resolve } from 'node:path'
import { performance } from 'node:perf_hooks'

const config = {
  baseUrl: process.env.BASE_URL || 'http://127.0.0.1:8080',
  dealId: Number(process.env.DEAL_ID),
  users: Number(process.env.USERS || 1000),
  prepareConcurrency: Number(process.env.PREPARE_CONCURRENCY || 20),
  timeoutMs: Number(process.env.REQUEST_TIMEOUT_MS || 120000),
  reportPath: resolve(process.env.REPORT_PATH || `loadtest/report-${Date.now()}.json`),
}

function requireSafeTarget() {
  const url = new URL(config.baseUrl)
  if (process.env.LOAD_TEST_ISOLATED !== 'YES')
    throw new Error('Set LOAD_TEST_ISOLATED=YES only after verifying the target uses a disposable database and Redis.')
  if (!['127.0.0.1', 'localhost', '::1'].includes(url.hostname))
    throw new Error('The load-test target must be loopback. Run it on the isolated test host.')
  if (!['http:', 'https:'].includes(url.protocol) || url.pathname !== '/' || url.search)
    throw new Error('BASE_URL must be a local origin, for example http://127.0.0.1:8081')
  if (!Number.isSafeInteger(config.dealId) || config.dealId <= 0)
    throw new Error('Set DEAL_ID to the isolated test deal ID.')
  if (!Number.isInteger(config.users) || config.users < 2 || config.users > 1000)
    throw new Error('USERS must be between 2 and 1000.')
  if (!Number.isInteger(config.prepareConcurrency) || config.prepareConcurrency < 1 || config.prepareConcurrency > 50)
    throw new Error('PREPARE_CONCURRENCY must be between 1 and 50.')
  if (!Number.isInteger(config.timeoutMs) || config.timeoutMs < 1000)
    throw new Error('REQUEST_TIMEOUT_MS must be at least 1000.')
  return url
}

const origin = requireSafeTarget()
const agent = origin.protocol === 'https:'
  ? new https.Agent({ keepAlive: true, maxSockets: config.users, maxFreeSockets: config.users })
  : new http.Agent({ keepAlive: true, maxSockets: config.users, maxFreeSockets: config.users })

function request(method, path, body, token) {
  const payload = body === undefined ? null : JSON.stringify(body)
  const url = new URL(path, origin)
  const client = url.protocol === 'https:' ? https : http
  const started = performance.now()
  return new Promise((resolveRequest, rejectRequest) => {
    const req = client.request(url, {
      method,
      agent,
      headers: {
        ...(payload ? { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(payload) } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    }, response => {
      const chunks = []
      response.on('data', chunk => chunks.push(chunk))
      response.on('end', () => {
        const raw = Buffer.concat(chunks).toString('utf8')
        let json
        try { json = JSON.parse(raw) } catch { json = { message: raw.slice(0, 200) } }
        resolveRequest({ status: response.statusCode, json, ms: performance.now() - started })
      })
    })
    req.setTimeout(config.timeoutMs, () => req.destroy(new Error(`request timeout after ${config.timeoutMs} ms`)))
    req.on('error', rejectRequest)
    if (payload) req.write(payload)
    req.end()
  })
}

function assertSuccess(result, action) {
  if (result.status !== 200 || result.json.code !== 200)
    throw new Error(`${action}: HTTP ${result.status}, ${result.json.message || 'unknown error'}`)
  return result.json.data
}

async function boundedMap(count, limit, callback) {
  const output = Array(count)
  let next = 0
  await Promise.all(Array.from({ length: Math.min(count, limit) }, async () => {
    while (next < count) {
      const index = next++
      output[index] = await callback(index)
    }
  }))
  return output
}

function percentile(values, fraction) {
  if (!values.length) return null
  const sorted = [...values].sort((a, b) => a - b)
  return Math.round(sorted[Math.ceil(sorted.length * fraction) - 1])
}

const prefix = `${Date.now()}_${randomBytes(3).toString('hex')}`
const phoneSeed = 200000 + randomBytes(3).readUIntBE(0, 3) % 700000
const password = `LoadTest!${randomBytes(10).toString('hex')}`

try {
  const offersBefore = assertSuccess(await request('GET', '/api/v1/daily-deals'), 'list deals')
  const offer = offersBefore.find(item => Number(item.id) === config.dealId)
  if (!offer) throw new Error('Selected deal is not visible; enable it and check dates and room-price/inventory data.')
  const now = Date.now()
  if (now < Date.parse(offer.startTime) || now >= Date.parse(offer.endTime))
    throw new Error('Selected deal is not currently in its flash-sale window.')
  if (Date.parse(offer.endTime) - now < 120000)
    throw new Error('Deal must remain open for at least two more minutes before starting.')
  if (offer.remaining < 1 || offer.remaining >= config.users)
    throw new Error(`Remaining quota/stock must be between 1 and ${config.users - 1}; got ${offer.remaining}.`)

  console.log(`Preparing ${config.users} separate customer accounts (excluded from timed burst)...`)
  const customers = await boundedMap(config.users, config.prepareConcurrency, async index => {
    const username = `load_${prefix}_${index}`
    const phone = `1${String(phoneSeed * 10000 + index).padStart(10, '0')}`
    const name = `压测客户${index + 1}`
    const account = assertSuccess(await request('POST', '/api/v1/auth/loadtest/register',
      { username, password, name, phone }), `prepare account ${index} (enable the loadtest backend profile)`)
    if (!account.token || account.role !== 'CUSTOMER')
      throw new Error(`Account ${index} did not receive a customer token.`)
    return { token: account.token, name, phone }
  })

  // Preparation can be slow due to BCrypt. Recheck the deal immediately before the burst.
  const refreshed = assertSuccess(await request('GET', '/api/v1/daily-deals'), 'recheck deal')
    .find(item => Number(item.id) === config.dealId)
  if (!refreshed || refreshed.remaining < 1 ||
      Date.parse(refreshed.endTime) - Date.now() < 120000)
    throw new Error('Deal was exhausted or has less than two minutes left after preparing customers.')
  const initialRemaining = refreshed.remaining
  const booking = (customer, index) => ({
    roomTypeId: refreshed.roomTypeId,
    roomCount: 1,
    guestCount: 1,
    checkInDate: refreshed.checkInDate,
    checkOutDate: refreshed.checkOutDate,
    dailyDealId: config.dealId,
    couponId: null,
    contactName: customer.name,
    contactPhone: customer.phone,
    remark: `isolated-load-test-${prefix}-${index}`,
    guests: [{ guestName: customer.name, phone: customer.phone, idType: 'ID_CARD', idNumber: null }],
  })

  console.log(`Sending ${config.users} concurrent booking requests for deal ${config.dealId}...`)
  const burstStarted = performance.now()
  const outcomes = await Promise.all(customers.map(async (customer, index) => {
    const sentAt = performance.now()
    try {
      const response = await request('POST', '/api/v1/bookings', booking(customer, index), customer.token)
      return { index, status: response.status, code: response.json.code,
        message: response.json.message, order: response.json.data, ms: response.ms }
    } catch (error) {
      return { index, status: 0, code: 0, message: error.message, ms: performance.now() - sentAt }
    }
  }))
  const burstMs = Math.round(performance.now() - burstStarted)
  const successful = outcomes.filter(item => item.status === 200 && item.code === 200 && item.order)
  const expectedRejections = outcomes.filter(item => item.status === 409 &&
    /秒杀名额已抢完|库存不足/.test(item.message || ''))
  const unexpected = outcomes.filter(item => !successful.includes(item) && !expectedRejections.includes(item))
  const ids = successful.map(item => item.order.orderId)
  const priceErrors = successful.filter(item =>
    Number(item.order.dailyDealId) !== config.dealId || item.order.couponId != null ||
    item.order.promotionId != null || Math.round(Number(item.order.totalAmount) * 100) !==
      Math.round(Number(refreshed.dealPrice) * 100))

  let afterRemaining = null
  let finalReadError = null
  try {
    const after = assertSuccess(await request('GET', '/api/v1/daily-deals'), 'final deal list')
      .find(item => Number(item.id) === config.dealId)
    afterRemaining = after?.remaining ?? null
    if (afterRemaining === null) finalReadError = 'Selected deal is absent from the final deal list.'
  } catch (error) { finalReadError = error.message }

  const invariant = {
    noOversell: successful.length <= initialRemaining,
    filledAvailableQuota: successful.length === Math.min(config.users, initialRemaining),
    uniqueOrderIds: new Set(ids).size === ids.length,
    correctPriceAndNoStacking: priceErrors.length === 0,
    remainingDecreasedBySuccesses: afterRemaining === null ? null :
      afterRemaining === initialRemaining - successful.length,
  }
  const report = {
    run: prefix, target: origin.origin, dealId: config.dealId, users: config.users,
    roomTypeId: refreshed.roomTypeId, initialRemaining, afterRemaining, finalReadError,
    expectedDealPrice: refreshed.dealPrice, burstMs,
    successful: successful.length, expectedRejections: expectedRejections.length,
    unexpectedFailures: unexpected.length,
    latencyMs: {
      p50: percentile(outcomes.map(item => item.ms), 0.5),
      p95: percentile(outcomes.map(item => item.ms), 0.95),
      p99: percentile(outcomes.map(item => item.ms), 0.99),
      max: Math.round(Math.max(...outcomes.map(item => item.ms))),
    },
    invariant,
    unexpectedByReason: Object.fromEntries([...new Set(unexpected.map(item => `${item.status} ${item.message}`))]
      .map(reason => [reason, unexpected.filter(item => `${item.status} ${item.message}` === reason).length])),
  }
  await mkdir(dirname(config.reportPath), { recursive: true })
  await writeFile(config.reportPath, `${JSON.stringify(report, null, 2)}\n`, 'utf8')
  console.log(JSON.stringify(report, null, 2))
  console.log(`Report: ${config.reportPath}`)
  if (Object.values(invariant).some(value => value === false) || unexpected.length > 0 || finalReadError)
    process.exitCode = 1
} catch (error) {
  console.error(`Load test aborted: ${error.message}`)
  process.exitCode = 1
} finally {
  agent.destroy()
}
