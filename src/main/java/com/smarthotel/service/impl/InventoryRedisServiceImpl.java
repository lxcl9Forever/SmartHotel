package com.smarthotel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smarthotel.entity.RoomInventory;
import com.smarthotel.mapper.RoomInventoryMapper;
import com.smarthotel.service.InventoryRedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import java.util.ArrayList;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryRedisServiceImpl
        implements InventoryRedisService {

    /**
     * Redis库存Key前缀
     *
     * 最终格式：
     * hotel:inventory:{roomTypeId}:{date}
     */
    private static final String KEY_PREFIX =
            "hotel:inventory:";

    private final StringRedisTemplate stringRedisTemplate;

    private final RoomInventoryMapper roomInventoryMapper;

    private final DefaultRedisScript<Long> lockInventoryScript;

    private final DefaultRedisScript<Long> releaseInventoryScript;

    @Override
    public void initInventory() {

        /*
         * 1. 从MySQL读取全部每日房量
         */
        List<RoomInventory> inventories =
                roomInventoryMapper.selectList(
                        new LambdaQueryWrapper<>()
                );


        /*
         * 2. 一条一条同步到Redis
         */
        for (RoomInventory inventory : inventories) {

            /*
             * 当前真正可售库存
             *
             * available =
             * total - locked - sold
             */
            int availableQty =
                    inventory.getTotalQty()
                            - inventory.getLockedQty()
                            - inventory.getSoldQty();


            /*
             * 例如：
             *
             * hotel:inventory:1:2026-10-20
             */
            String key =
                    buildKey(
                            inventory.getRoomTypeId(),
                            inventory.getInventoryDate()
                    );


            /*
             * Redis中直接保存可售数量
             */
            stringRedisTemplate
                    .opsForValue()
                    .set(
                            key,
                            String.valueOf(availableQty)
                    );
        }
    }


    @Override
    public Integer getAvailableQty(
            Long roomTypeId,
            LocalDate date) {

        String key =
                buildKey(
                        roomTypeId,
                        date
                );

        String value =
                stringRedisTemplate
                        .opsForValue()
                        .get(key);

        if (value == null) {
            return null;
        }

        return Integer.valueOf(value);
    }


    /**
     * 构造Redis库存Key
     */
    private String buildKey(
            Long roomTypeId,
            LocalDate date) {

        return KEY_PREFIX
                + roomTypeId
                + ":"
                + date;
    }

    @Override
    public Long lockInventory(
            Long roomTypeId,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            Integer roomCount) {

        /*
         * 1. 基础参数检查
         */
        if (roomTypeId == null
                || checkInDate == null
                || checkOutDate == null
                || roomCount == null) {

            return -2L;
        }

        if (!checkInDate.isBefore(checkOutDate)
                || roomCount <= 0) {

            return -2L;
        }


        /*
         * 2. 构造整个入住期间的Redis Key
         *
         * 例如：
         *
         * hotel:inventory:1:2026-10-25
         * hotel:inventory:1:2026-10-26
         * hotel:inventory:1:2026-10-27
         */
        List<String> keys =
                new ArrayList<>();

        LocalDate currentDate =
                checkInDate;

        while (currentDate.isBefore(checkOutDate)) {

            keys.add(
                    buildKey(
                            roomTypeId,
                            currentDate
                    )
            );

            currentDate =
                    currentDate.plusDays(1);
        }


        /*
         * 3. Redis一次性执行Lua脚本
         */
        Long result =
                stringRedisTemplate.execute(
                        lockInventoryScript,
                        keys,
                        String.valueOf(roomCount)
                );


        /*
         * 理论上不会为null，
         * 这里做防御处理
         */
        if (result == null) {
            return -2L;
        }

        return result;
    }

    @Override
    public Long releaseInventory(
            Long roomTypeId,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            Integer roomCount) {


        if (roomTypeId == null
                || checkInDate == null
                || checkOutDate == null
                || roomCount == null) {

            return -2L;
        }


        if (!checkInDate.isBefore(checkOutDate)
                || roomCount <= 0) {

            return -2L;
        }


        List<String> keys =
                new ArrayList<>();


        LocalDate currentDate =
                checkInDate;


        while (currentDate.isBefore(checkOutDate)) {


            keys.add(
                    buildKey(
                            roomTypeId,
                            currentDate
                    )
            );


            currentDate =
                    currentDate.plusDays(1);
        }


        Long result =
                stringRedisTemplate.execute(
                        releaseInventoryScript,
                        keys,
                        String.valueOf(roomCount)
                );


        if(result == null){
            return -2L;
        }


        return result;
    }
}