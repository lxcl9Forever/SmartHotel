package com.smarthotel.controller;

import com.smarthotel.common.result.Result;
import com.smarthotel.service.InventoryRedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/test/redis")
@RequiredArgsConstructor
public class RedisTestController {

    private final StringRedisTemplate stringRedisTemplate;

    private final InventoryRedisService inventoryRedisService;


    /**
     * 基础Redis连通性测试
     */
    @GetMapping
    public Result<String> testRedis() {

        stringRedisTemplate
                .opsForValue()
                .set(
                        "smarthotel:test",
                        "hello redis"
                );

        String value =
                stringRedisTemplate
                        .opsForValue()
                        .get(
                                "smarthotel:test"
                        );

        return Result.success(value);
    }


    /**
     * 将MySQL每日库存初始化到Redis
     */
    @PostMapping("/inventory/init")
    public Result<Void> initInventory() {

        inventoryRedisService.initInventory();

        return Result.success();
    }


    /**
     * 查询Redis中的某一天可售库存
     */
    @GetMapping("/inventory")
    public Result<Integer> getInventory(
            @RequestParam Long roomTypeId,
            @RequestParam LocalDate date) {

        return Result.success(
                inventoryRedisService.getAvailableQty(
                        roomTypeId,
                        date
                )
        );
    }

    @PostMapping("/inventory/lock")
    public Result<Long> lockInventory(
            @RequestParam Long roomTypeId,
            @RequestParam LocalDate checkInDate,
            @RequestParam LocalDate checkOutDate,
            @RequestParam Integer roomCount) {

        Long result =
                inventoryRedisService.lockInventory(
                        roomTypeId,
                        checkInDate,
                        checkOutDate,
                        roomCount
                );

        return Result.success(result);
    }
}