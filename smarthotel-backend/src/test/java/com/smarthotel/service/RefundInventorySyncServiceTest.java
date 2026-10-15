package com.smarthotel.service;

import com.smarthotel.entity.BookingOrder;
import com.smarthotel.entity.RefundPaymentRecord;
import com.smarthotel.mapper.BookingOrderMapper;
import com.smarthotel.mapper.RefundPaymentRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.mockito.Mockito.*;

class RefundInventorySyncServiceTest {

    private final RefundPaymentRecordMapper records = mock(RefundPaymentRecordMapper.class);
    private final BookingOrderMapper orders = mock(BookingOrderMapper.class);
    private final InventoryRedisService redis = mock(InventoryRedisService.class);
    private RefundInventorySyncService service;

    @BeforeEach
    void setUp() {
        service = new RefundInventorySyncService(records, orders, redis);
    }

    @Test
    void successfulRedisReleaseMarksLedgerSynced() {
        stubPending();
        when(redis.releaseRefundedInventoryOnce(
                5L, 14L, LocalDate.of(2026, 10, 13),
                LocalDate.of(2026, 10, 14), 1)).thenReturn(1L);

        service.syncOne(5L);

        verify(records).markInventorySynced(5L);
    }

    @Test
    void missingRedisKeysLeaveLedgerPendingForRetry() {
        stubPending();
        when(redis.releaseRefundedInventoryOnce(
                5L, 14L, LocalDate.of(2026, 10, 13),
                LocalDate.of(2026, 10, 14), 1)).thenReturn(-2L);

        service.syncOne(5L);

        verify(records, never()).markInventorySynced(any());
    }

    private void stubPending() {
        RefundPaymentRecord record = new RefundPaymentRecord();
        record.setRefundId(5L);
        record.setOrderId(30L);
        record.setInventorySyncStatus("PENDING");
        when(records.selectByRefundId(5L)).thenReturn(record);

        BookingOrder order = new BookingOrder();
        order.setOrderId(30L);
        order.setRoomTypeId(14L);
        order.setRoomCount(1);
        order.setCheckInDate(LocalDate.of(2026, 10, 13));
        order.setCheckOutDate(LocalDate.of(2026, 10, 14));
        when(orders.selectById(30L)).thenReturn(order);
    }
}
