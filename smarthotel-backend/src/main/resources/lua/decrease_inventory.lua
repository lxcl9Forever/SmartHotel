for i,key in ipairs(KEYS)
do

    local stock =
    tonumber(redis.call('get',key))


    if stock < tonumber(ARGV[1])
    then
        return 0
    end

end


for i,key in ipairs(KEYS)
do

    redis.call(
        'decrby',
        key,
        ARGV[1]
    )

end


return 1