package com.smarthotel.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@Configuration
public class RedisScriptConfig {


    @Bean
    public DefaultRedisScript<Long> lockInventoryScript() {

        DefaultRedisScript<Long> script =
                new DefaultRedisScript<>();

        script.setLocation(
                new ClassPathResource(
                        "lua/lock_inventory.lua"
                )
        );

        script.setResultType(Long.class);

        return script;
    }



    @Bean
    public DefaultRedisScript<Long> releaseInventoryScript() {

        DefaultRedisScript<Long> script =
                new DefaultRedisScript<>();

        script.setLocation(
                new ClassPathResource(
                        "lua/release_inventory.lua"
                )
        );

        script.setResultType(Long.class);

        return script;
    }

    @Bean
    public DefaultRedisScript<Long> decreaseInventoryScript() {


        DefaultRedisScript<Long> script =
                new DefaultRedisScript<>();


        script.setLocation(
                new ClassPathResource(
                        "lua/decrease_inventory.lua"
                )
        );


        script.setResultType(Long.class);


        return script;
    }

    @Bean
    public DefaultRedisScript<Long> refundReleaseInventoryScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("lua/release_refunded_inventory.lua"));
        script.setResultType(Long.class);
        return script;
    }
}
