package com.smarthotel.service;

import com.smarthotel.entity.BookingOrder;
import com.smarthotel.entity.RoomInventory;
import com.smarthotel.mapper.BookingOrderMapper;
import com.smarthotel.mapper.RoomInventoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderTimeoutServiceTest {
    private BookingOrderMapper orderMapper;
    private RoomInventoryMapper inventoryMapper;
    private InventoryRedisService redisService;
    private DailyCheckinService checkinService;
    private OrderTimeoutService service;
    private BookingOrder order;
    private RoomInventory inventory;

    @BeforeEach
    void setUp() {
        orderMapper = mock(BookingOrderMapper.class);
        inventoryMapper = mock(RoomInventoryMapper.class);
        redisService = mock(InventoryRedisService.class);
        checkinService = mock(DailyCheckinService.class);
        service = new OrderTimeoutService(orderMapper, inventoryMapper, redisService,
                checkinService, new NoopTransactionManager());

        order = new BookingOrder();
        order.setOrderId(7L);
        order.setRoomTypeId(2L);
        order.setRoomCount(1);
        order.setNights(1);
        order.setCheckInDate(LocalDate.now().plusDays(1));
        order.setCheckOutDate(LocalDate.now().plusDays(2));
        order.setExpireTime(LocalDateTime.now().minusMinutes(1));
        order.setOrderStatus("WAIT_PAY");
        order.setPaymentStatus("UNPAID");

        inventory = new RoomInventory();
        inventory.setLockedQty(1);
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(inventoryMapper.selectList(any())).thenReturn(List.of(inventory));
        when(redisService.releaseInventory(any(), any(), any(), any())).thenReturn(1L);
    }

    @Test
    void closesExpiredUnpaidOrderOnceAndReleasesInventoryAfterCommit() {
        service.closeIfExpired(7L);
        service.closeIfExpired(7L);

        assertEquals("CLOSED", order.getOrderStatus());
        assertEquals(0, inventory.getLockedQty());
        verify(inventoryMapper, times(1)).updateById(inventory);
        verify(orderMapper, times(1)).updateById(order);
        verify(redisService, times(1)).releaseInventory(2L, order.getCheckInDate(), order.getCheckOutDate(), 1);
        verify(checkinService, times(1)).release(7L);
    }

    @Test
    void doesNotClosePaidOrder() {
        order.setPaymentStatus("PAID");
        service.closeIfExpired(7L);

        assertEquals("WAIT_PAY", order.getOrderStatus());
        verify(inventoryMapper, never()).selectList(any());
        verifyNoInteractions(redisService);
        verifyNoInteractions(checkinService);
    }

    @Test
    void doesNotCloseOrderBeforeExpiry() {
        order.setExpireTime(LocalDateTime.now().plusMinutes(2));
        service.closeIfExpired(7L);

        assertEquals("WAIT_PAY", order.getOrderStatus());
        verify(inventoryMapper, never()).selectList(any());
    }

    private static class NoopTransactionManager extends AbstractPlatformTransactionManager {
        @Override
        protected Object doGetTransaction() { return new Object(); }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) { }

        @Override
        protected void doCommit(DefaultTransactionStatus status) { }

        @Override
        protected void doRollback(DefaultTransactionStatus status) { }
    }
}
