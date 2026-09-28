for i,key in ipairs(KEYS) do

    local value = redis.call('get', key)

    if value then

        redis.call(
            'set',
            key,
            tonumber(value) + tonumber(ARGV[1])
        )

    end

end


return 1