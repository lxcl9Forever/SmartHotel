package com.smarthotel.service.impl;

import com.smarthotel.entity.BookingOrder;
import com.smarthotel.entity.Payment;
import com.smarthotel.entity.RefundRecord;
import com.smarthotel.entity.RefundPaymentRecord;
import com.smarthotel.entity.RoomInventory;
import com.smarthotel.exception.BusinessException;
import com.smarthotel.mapper.BookingOrderMapper;
import com.smarthotel.mapper.PaymentMapper;
import com.smarthotel.mapper.RefundRecordMapper;
import com.smarthotel.mapper.RefundPaymentRecordMapper;
import com.smarthotel.mapper.RoomInventoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminRefundServiceImplTest {

    private final RefundRecordMapper refunds = mock(RefundRecordMapper.class);
    private final BookingOrderMapper orders = mock(BookingOrderMapper.class);
    private final PaymentMapper payments = mock(PaymentMapper.class);
    private final RoomInventoryMapper inventories = mock(RoomInventoryMapper.class);
    private final RefundPaymentRecordMapper refundPayments = mock(RefundPaymentRecordMapper.class);
    private AdminRefundServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminRefundServiceImpl(refunds, orders, payments, inventories, refundPayments);
    }

    @Test
    void approveMarksRefundAndOrderRefunded() {
        BookingOrder order = pendingOrder();
        stubPendingRefund(order);
        Payment payment = new Payment();
        payment.setAmount(new BigDecimal("1588.00"));
        when(payments.selectList(any())).thenReturn(List.of(payment));
        RoomInventory inventory = new RoomInventory();
        inventory.setSoldQty(1);
        when(inventories.selectList(any())).thenReturn(List.of(inventory));
        when(inventories.updateById(inventory)).thenReturn(1);
        when(refundPayments.insert(any(RefundPaymentRecord.class))).thenReturn(1);
        when(refunds.updateStatus(5L, "APPROVED")).thenReturn(1);
        when(orders.updateById(order)).thenReturn(1);

        service.approve(5L);

        assertEquals("REFUNDED", order.getOrderStatus());
        assertEquals("REFUNDED", order.getPaymentStatus());
        assertEquals(0, inventory.getSoldQty());
        verify(refunds).updateStatus(5L, "APPROVED");
        verify(refundPayments).insert(argThat(record ->
                record.getRefundId().equals(5L)
                        && record.getRefundAmount().compareTo(new BigDecimal("1588.00")) == 0
                        && "PENDING".equals(record.getInventorySyncStatus())));
    }

    @Test
    void rejectRestoresPaidState() {
        BookingOrder order = pendingOrder();
        stubPendingRefund(order);
        when(refunds.updateStatus(5L, "REJECTED")).thenReturn(1);
        when(orders.updateById(order)).thenReturn(1);

        service.reject(5L);

        assertEquals("CONFIRMED", order.getOrderStatus());
        assertEquals("PAID", order.getPaymentStatus());
        verify(refunds).updateStatus(5L, "REJECTED");
        verifyNoInteractions(payments, inventories, refundPayments);
    }

    @Test
    void repeatedReviewReturnsConflictWithoutChangingOrder() {
        RefundRecord refund = pendingRefund();
        refund.setRefundStatus("APPROVED");
        when(refunds.selectById(5L)).thenReturn(refund);
        when(refunds.selectByIdForUpdate(5L)).thenReturn(refund);
        when(orders.selectOne(any())).thenReturn(pendingOrder());

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.reject(5L));

        assertEquals(409, error.getCode());
        verify(refunds, never()).updateStatus(any(), any());
        verify(orders, never()).updateById(any(BookingOrder.class));
    }

    @Test
    void approvalWithoutSoldInventoryCannotCreateRefundLedger() {
        stubPendingRefund(pendingOrder());
        Payment payment = new Payment();
        payment.setAmount(new BigDecimal("1588.00"));
        when(payments.selectList(any())).thenReturn(List.of(payment));
        RoomInventory inventory = new RoomInventory();
        inventory.setSoldQty(0);
        when(inventories.selectList(any())).thenReturn(List.of(inventory));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.approve(5L));

        assertEquals(409, error.getCode());
        verifyNoInteractions(refundPayments);
        verify(refunds, never()).updateStatus(any(), any());
    }

    private void stubPendingRefund(BookingOrder order) {
        RefundRecord refund = pendingRefund();
        when(refunds.selectById(5L)).thenReturn(refund);
        when(refunds.selectByIdForUpdate(5L)).thenReturn(refund);
        when(orders.selectOne(any())).thenReturn(order);
    }

    private RefundRecord pendingRefund() {
        RefundRecord refund = new RefundRecord();
        refund.setRefundId(5L);
        refund.setOrderId(30L);
        refund.setRefundStatus("PENDING");
        refund.setRefundAmount(new BigDecimal("1588.00"));
        return refund;
    }

    private BookingOrder pendingOrder() {
        BookingOrder order = new BookingOrder();
        order.setOrderId(30L);
        order.setOrderStatus("CONFIRMED");
        order.setPaymentStatus("REFUNDING");
        order.setRoomTypeId(14L);
        order.setRoomCount(1);
        order.setNights(1);
        order.setCheckInDate(LocalDate.of(2026, 10, 13));
        order.setCheckOutDate(LocalDate.of(2026, 10, 14));
        return order;
    }
}
