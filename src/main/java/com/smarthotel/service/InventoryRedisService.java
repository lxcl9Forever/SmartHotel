package com.smarthotel.service;

import java.time.LocalDate;

public interface InventoryRedisService {

    void initInventory();

    Integer getAvailableQty(
            Long roomTypeId,
            LocalDate date
    );

    /**
     * 使用Lua原子锁定整个入住区间库存
     *
     * 1  = 成功
     * -1 = 库存不足
     * -2 = Redis库存不存在
     */
    Long lockInventory(
            Long roomTypeId,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            Integer roomCount
    );

    /**
     * 释放Redis库存
     */
    Long releaseInventory(
            Long roomTypeId,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            Integer roomCount
    );
}