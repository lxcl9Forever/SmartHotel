local roomCount = tonumber(ARGV[1])

-- 第一轮：检查所有日期库存
for i = 1, #KEYS do

    local stock = redis.call('GET', KEYS[i])

    -- Redis中没有这个日期库存
    if not stock then
        return -2
    end

    stock = tonumber(stock)

    -- 任意一天库存不足，整个操作失败
    if stock < roomCount then
        return -1
    end
end


-- 第二轮：确认全部够，再统一扣减
for i = 1, #KEYS do

    redis.call(
        'DECRBY',
        KEYS[i],
        roomCount
    )
end


return 1