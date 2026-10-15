package com.smarthotel.service.impl;

import com.smarthotel.entity.BookingOrder;
import com.smarthotel.entity.Customer;
import com.smarthotel.entity.RefundRecord;
import com.smarthotel.exception.BusinessException;
import com.smarthotel.mapper.BookingOrderMapper;
import com.smarthotel.mapper.CustomerMapper;
import com.smarthotel.mapper.RefundRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RefundServiceImplTest {

    private final RefundRecordMapper refundRecordMapper = mock(RefundRecordMapper.class);
    private final BookingOrderMapper bookingOrderMapper = mock(BookingOrderMapper.class);
    private final CustomerMapper customerMapper = mock(CustomerMapper.class);
    private RefundServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RefundServiceImpl(refundRecordMapper, bookingOrderMapper, customerMapper);
    }

    @Test
    void firstApplicationCreatesRecordAndMarksPaymentRefunding() {
        BookingOrder order = ownedPaidOrder();
        when(bookingOrderMapper.selectOne(any())).thenReturn(order);
        when(customerMapper.selectOne(any())).thenReturn(owner());

        service.applyRefund(30L, 3L, "  行程变更  ");

        verify(refundRecordMapper).insert(argThat(record ->
                record.getOrderId().equals(30L)
                        && record.getRefundAmount().compareTo(new BigDecimal("1588.00")) == 0
                        && record.getRefundReason().equals("行程变更")
                        && record.getRefundStatus().equals("PENDING")));
        assertEquals("REFUNDING", order.getPaymentStatus());
        verify(bookingOrderMapper).updateById(order);
    }

    @Test
    void secondApplicationReturnsConflictWithoutWritingAgain() {
        when(bookingOrderMapper.selectOne(any())).thenReturn(ownedPaidOrder());
        when(customerMapper.selectOne(any())).thenReturn(owner());
        when(refundRecordMapper.selectByOrderId(eq(30L))).thenReturn(new RefundRecord());

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.applyRefund(30L, 3L, "行程变更"));

        assertEquals(409, error.getCode());
        assertEquals("该订单已经申请退款，请勿重复申请", error.getMessage());
        verify(refundRecordMapper, never()).insert(any());
        verify(bookingOrderMapper, never()).updateById(any(BookingOrder.class));
    }

    @Test
    void rejectedApplicationCanBeSubmittedAgain() {
        BookingOrder order = ownedPaidOrder();
        RefundRecord rejected = new RefundRecord();
        rejected.setRefundStatus("REJECTED");
        when(bookingOrderMapper.selectOne(any())).thenReturn(order);
        when(customerMapper.selectOne(any())).thenReturn(owner());
        when(refundRecordMapper.selectByOrderId(eq(30L))).thenReturn(rejected);

        service.applyRefund(30L, 3L, "补充退款原因");

        verify(refundRecordMapper).insert(argThat(record ->
                "PENDING".equals(record.getRefundStatus())
                        && "补充退款原因".equals(record.getRefundReason())));
        assertEquals("REFUNDING", order.getPaymentStatus());
        verify(bookingOrderMapper).updateById(order);
    }

    private BookingOrder ownedPaidOrder() {
        BookingOrder order = new BookingOrder();
        order.setOrderId(30L);
        order.setCustomerId(1L);
        order.setOrderStatus("CONFIRMED");
        order.setPaymentStatus("PAID");
        order.setTotalAmount(new BigDecimal("1588.00"));
        return order;
    }

    private Customer owner() {
        Customer customer = new Customer();
        customer.setCustomerId(1L);
        return customer;
    }
}
