package com.smarthotel.service.impl;

import com.smarthotel.dto.stay.ExtendStayRequest;
import com.smarthotel.entity.BookingOrder;
import com.smarthotel.entity.Customer;
import com.smarthotel.entity.Payment;
import com.smarthotel.entity.RoomInventory;
import com.smarthotel.entity.RoomPrice;
import com.smarthotel.exception.BusinessException;
import com.smarthotel.mapper.BookingOrderMapper;
import com.smarthotel.mapper.CustomerMapper;
import com.smarthotel.mapper.PaymentMapper;
import com.smarthotel.mapper.RoomInventoryMapper;
import com.smarthotel.mapper.RoomPriceMapper;
import com.smarthotel.service.InventoryRedisService;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class StayExtensionServiceImplTest {
    private final BookingOrderMapper orders = mock(BookingOrderMapper.class);
    private final CustomerMapper customers = mock(CustomerMapper.class);
    private final RoomPriceMapper prices = mock(RoomPriceMapper.class);
    private final RoomInventoryMapper inventories = mock(RoomInventoryMapper.class);
    private final InventoryRedisService redis = mock(InventoryRedisService.class);
    private final PaymentMapper payments = mock(PaymentMapper.class);
    private final StayExtensionServiceImpl service = new StayExtensionServiceImpl(
            orders, customers, prices, inventories, redis, payments);

    @Test
    void customerCannotPreviewOrExtendAnotherCustomersOrder() {
        Customer customer = new Customer();
        customer.setCustomerId(11L);
        when(customers.selectOne(any())).thenReturn(customer);
        BookingOrder order = checkedInOrder();
        order.setCustomerId(22L);
        when(orders.selectOne(any())).thenReturn(order);
        ExtendStayRequest request = request();

        assertEquals(404, assertThrows(BusinessException.class,
                () -> service.previewForCustomer(1L, request)).getCode());
        assertEquals(404, assertThrows(BusinessException.class,
                () -> service.extendForCustomer(1L, request)).getCode());
        verifyNoInteractions(prices, inventories, redis, payments);
    }

    @Test
    void extensionChargesAllRoomsAndReservesEachNewNight() {
        Customer customer = new Customer();
        customer.setCustomerId(11L);
        when(customers.selectOne(any())).thenReturn(customer);
        BookingOrder order = checkedInOrder();
        when(orders.selectOne(any())).thenReturn(order);
        RoomPrice firstPrice = price("100.00");
        RoomPrice secondPrice = price("150.00");
        when(prices.selectList(any())).thenReturn(List.of(firstPrice, secondPrice));
        RoomInventory firstDay = inventory();
        RoomInventory secondDay = inventory();
        when(inventories.selectList(any())).thenReturn(List.of(firstDay, secondDay));
        when(redis.lockInventory(eq(3L), any(), any(), eq(2))).thenReturn(1L);
        when(inventories.updateById(any(RoomInventory.class))).thenReturn(1);
        when(orders.updateById(any(BookingOrder.class))).thenReturn(1);
        when(payments.insert(any(Payment.class))).thenReturn(1);

        TransactionSynchronizationManager.initSynchronization();
        try {
            var result = service.extendForCustomer(1L, request());
            assertTrue(result.getSuccess());
            assertEquals(new BigDecimal("500.00"), result.getExtraAmount());
            assertEquals(3, firstDay.getSoldQty());
            assertEquals(3, secondDay.getSoldQty());
            assertEquals(new BigDecimal("900.00"), order.getTotalAmount());
            assertEquals(4, order.getNights());
            var payment = org.mockito.ArgumentCaptor.forClass(Payment.class);
            verify(payments).insert(payment.capture());
            assertEquals("SUCCESS", payment.getValue().getPaymentStatus());
            assertEquals(new BigDecimal("500.00"), payment.getValue().getAmount());
            verify(redis).lockInventory(3L, LocalDate.now().plusDays(1),
                    LocalDate.now().plusDays(3), 2);
            verify(redis, never()).releaseInventory(any(), any(), any(), any());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void rollbackReleasesReservedRedisInventory() {
        Customer customer = new Customer();
        customer.setCustomerId(11L);
        when(customers.selectOne(any())).thenReturn(customer);
        when(orders.selectOne(any())).thenReturn(checkedInOrder());
        when(prices.selectList(any())).thenReturn(List.of(price("100"), price("100")));
        when(inventories.selectList(any())).thenReturn(List.of(inventory(), inventory()));
        when(redis.lockInventory(any(), any(), any(), any())).thenReturn(1L);
        when(redis.releaseInventory(any(), any(), any(), any())).thenReturn(1L);
        when(inventories.updateById(any(RoomInventory.class))).thenReturn(1);
        when(orders.updateById(any(BookingOrder.class))).thenReturn(1);
        when(payments.insert(any(Payment.class))).thenReturn(1);

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.extendForCustomer(1L, request());
            for (TransactionSynchronization synchronization
                    : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
            }
            verify(redis).releaseInventory(3L, LocalDate.now().plusDays(1),
                    LocalDate.now().plusDays(3), 2);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private BookingOrder checkedInOrder() {
        BookingOrder order = new BookingOrder();
        order.setOrderId(7L);
        order.setCustomerId(11L);
        order.setRoomTypeId(3L);
        order.setRoomCount(2);
        order.setCheckOutDate(LocalDate.now().plusDays(1));
        order.setNights(2);
        order.setTotalAmount(new BigDecimal("400.00"));
        order.setOrderStatus("CHECKED_IN");
        order.setPaymentStatus("PAID");
        return order;
    }

    private ExtendStayRequest request() {
        ExtendStayRequest request = new ExtendStayRequest();
        request.setOrderId(7L);
        request.setNewCheckOutDate(LocalDate.now().plusDays(3));
        return request;
    }

    private RoomPrice price(String amount) {
        RoomPrice price = new RoomPrice();
        price.setSalePrice(new BigDecimal(amount));
        return price;
    }

    private RoomInventory inventory() {
        RoomInventory day = new RoomInventory();
        day.setTotalQty(5);
        day.setLockedQty(0);
        day.setSoldQty(1);
        return day;
    }
}
