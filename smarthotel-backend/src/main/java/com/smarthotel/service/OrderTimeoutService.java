package com.smarthotel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smarthotel.entity.BookingOrder;
import com.smarthotel.entity.RoomInventory;
import com.smarthotel.mapper.BookingOrderMapper;
import com.smarthotel.mapper.RoomInventoryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class OrderTimeoutService {
    private final BookingOrderMapper bookingOrderMapper;
    private final RoomInventoryMapper roomInventoryMapper;
    private final InventoryRedisService inventoryRedisService;
    private final DailyCheckinService dailyCheckinService;
    private final TransactionTemplate transactionTemplate;

    public OrderTimeoutService(BookingOrderMapper bookingOrderMapper,
                               RoomInventoryMapper roomInventoryMapper,
                               InventoryRedisService inventoryRedisService,
                               DailyCheckinService dailyCheckinService,
                               PlatformTransactionManager transactionManager) {
        this.bookingOrderMapper = bookingOrderMapper;
        this.roomInventoryMapper = roomInventoryMapper;
        this.inventoryRedisService = inventoryRedisService;
        this.dailyCheckinService = dailyCheckinService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public void scanExpired() {
        List<BookingOrder> candidates = bookingOrderMapper.selectList(
                new LambdaQueryWrapper<BookingOrder>()
                        .eq(BookingOrder::getOrderStatus, "WAIT_PAY")
                        .eq(BookingOrder::getPaymentStatus, "UNPAID")
                        .le(BookingOrder::getExpireTime, LocalDateTime.now())
                        .orderByAsc(BookingOrder::getExpireTime)
                        .last("LIMIT 200"));
        for (BookingOrder candidate : candidates) {
            try {
                closeIfExpired(candidate.getOrderId());
            } catch (RuntimeException error) {
                log.error("Failed to close expired order {}; next scan will retry", candidate.getOrderId(), error);
            }
        }
    }

    public void closeIfExpired(Long orderId) {
        transactionTemplate.executeWithoutResult(status -> closeLocked(orderId));
    }

    private void closeLocked(Long orderId) {
        BookingOrder order = bookingOrderMapper.selectOne(
                new LambdaQueryWrapper<BookingOrder>()
                        .eq(BookingOrder::getOrderId, orderId)
                        .last("FOR UPDATE"));
        if (order == null || !"WAIT_PAY".equals(order.getOrderStatus())
                || !"UNPAID".equals(order.getPaymentStatus())
                || order.getExpireTime() == null
                || order.getExpireTime().isAfter(LocalDateTime.now())) {
            return;
        }

        List<RoomInventory> inventories = roomInventoryMapper.selectList(
                new LambdaQueryWrapper<RoomInventory>()
                        .eq(RoomInventory::getRoomTypeId, order.getRoomTypeId())
                        .ge(RoomInventory::getInventoryDate, order.getCheckInDate())
                        .lt(RoomInventory::getInventoryDate, order.getCheckOutDate())
                        .orderByAsc(RoomInventory::getInventoryDate)
                        .last("FOR UPDATE"));
        if (inventories.size() != order.getNights()) {
            throw new IllegalStateException("订单 " + orderId + " 的库存日期不完整");
        }
        for (RoomInventory inventory : inventories) {
            if (inventory.getLockedQty() < order.getRoomCount()) {
                throw new IllegalStateException("订单 " + orderId + " 的库存锁定数量异常");
            }
            inventory.setLockedQty(inventory.getLockedQty() - order.getRoomCount());
            roomInventoryMapper.updateById(inventory);
        }
        order.setOrderStatus("CLOSED");
        bookingOrderMapper.updateById(order);
        dailyCheckinService.release(orderId);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    Long result = inventoryRedisService.releaseInventory(
                            order.getRoomTypeId(), order.getCheckInDate(),
                            order.getCheckOutDate(), order.getRoomCount());
                    if (result == null || result != 1L) {
                        log.warn("Redis inventory release for expired order {} returned {}", orderId, result);
                    }
                } catch (RuntimeException error) {
                    log.error("Redis inventory release failed for expired order {}; resync inventory", orderId, error);
                }
            }
        });
    }
}
