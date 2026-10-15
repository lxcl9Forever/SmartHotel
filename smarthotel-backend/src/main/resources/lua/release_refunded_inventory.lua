-- KEYS[1] is a permanent idempotency marker for this refund.
-- Remaining keys are the daily available-room counters.
if redis.call('EXISTS', KEYS[1]) == 1 then
    return 1
end

for i = 2, #KEYS do
    if not redis.call('GET', KEYS[i]) then
        return -2
    end
end

for i = 2, #KEYS do
    redis.call('INCRBY', KEYS[i], tonumber(ARGV[1]))
end

redis.call('SET', KEYS[1], '1')
return 1
