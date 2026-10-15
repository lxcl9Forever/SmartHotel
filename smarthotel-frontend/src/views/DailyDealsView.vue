<template>
  <main class="page">
    <div class="top"><el-button @click="router.push('/home')">返回首页</el-button><h1>每日优惠</h1></div>
    <p class="hint">限量房型先到先得；秒杀订单不能使用优惠券，也不参与其他优惠活动。价格与剩余名额以提交订单时为准。</p>
    <div v-loading="loading" class="offers">
      <el-empty v-if="!loading && !offers.length" description="暂无每日优惠" />
      <article v-for="offer in pagedOffers" :key="offer.id" class="offer">
        <img v-if="getRoomCover(offer.typeCode)" :src="getRoomCover(offer.typeCode)" :alt="offer.roomTypeName" />
        <div class="details">
          <el-tag type="danger">限时秒杀 · {{ Number(offer.discountRate) * 10 }} 折</el-tag>
          <h2>{{ offer.roomTypeName }}</h2>
          <p>{{ offer.checkInDate }} 入住 · {{ offer.checkOutDate }} 退房</p>
          <p>活动时间：{{ time(offer.startTime) }} 至 {{ time(offer.endTime) }}</p>
          <p>剩余名额：{{ offer.remaining }} 间</p>
        </div>
        <div class="action">
          <del>¥{{ offer.originalPrice }}</del>
          <strong>¥{{ offer.dealPrice }}</strong>
          <span>/ 间，整个入住期间</span>
          <el-button type="danger" size="large" :disabled="!canBook(offer)" @click="book(offer)">
            {{ offer.remaining <= 0 ? '已抢完' : now < new Date(offer.startTime).getTime() ? '尚未开抢' : '抢着订房' }}
          </el-button>
        </div>
      </article>
    </div>
    <AppPagination v-model:current-page="currentPage" v-model:page-size="pageSize" :total="offers.length" />
  </main>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import AppPagination from '../components/AppPagination.vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listDailyDeals } from '../api/dailyDeal'
import { getRoomCover } from '../data/roomMedia'

const router = useRouter()
const offers = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const pagedOffers = computed(() => offers.value.slice((currentPage.value - 1) * pageSize.value, currentPage.value * pageSize.value))
const loading = ref(false)
const now = ref(Date.now())
const time = value => value?.replace('T', ' ').slice(0, 16)
const canBook = offer => offer.remaining > 0 && now.value >= new Date(offer.startTime).getTime() && now.value < new Date(offer.endTime).getTime()
const book = offer => router.push({ path: '/home', query: { dailyDealId: offer.id } })
let clockTimer
let refreshTimer
async function load(silent = false) {
  if (!silent) loading.value = true
  try {
    const { data } = await listDailyDeals()
    if (data.code !== 200) throw new Error(data.message)
    offers.value = data.data || []
  } catch (error) { if (!silent) ElMessage.error(error.response?.data?.message || error.message || '每日优惠加载失败') }
  finally { loading.value = false }
}
onMounted(() => {
  load()
  clockTimer = setInterval(() => { now.value = Date.now() }, 1000)
  refreshTimer = setInterval(() => load(true), 10000)
})
onUnmounted(() => {
  clearInterval(clockTimer)
  clearInterval(refreshTimer)
})
</script>

<style scoped>
.page { min-height: 100vh; padding: 36px 7%; background: #f5f7fa; color: #1f2937; }
.top { display: flex; align-items: center; gap: 20px; }
.top h1 { margin: 0; }
.hint { color: #667085; margin: 16px 0 26px; }
.offers { max-width: 1180px; }
.offer { display: flex; gap: 24px; align-items: center; padding: 24px; background: white; border-radius: 14px; margin-bottom: 18px; box-shadow: 0 8px 30px #0000000c; }
.offer img { width: 220px; height: 150px; object-fit: cover; border-radius: 10px; }
.details { flex: 1; }
.details h2 { margin: 10px 0; }
.details p { margin: 6px 0; color: #667085; }
.action { display: flex; flex-direction: column; align-items: flex-end; gap: 5px; }
.action del { color: #98a2b3; }
.action strong { font-size: 30px; color: #e53935; }
.action span { color: #667085; margin-bottom: 10px; }
@media (max-width: 780px) { .offer { flex-direction: column; align-items: stretch; } .offer img { width: 100%; } .action { align-items: flex-start; } }
</style>
