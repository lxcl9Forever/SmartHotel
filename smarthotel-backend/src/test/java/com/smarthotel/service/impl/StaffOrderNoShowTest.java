package com.smarthotel.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.smarthotel.entity.BookingOrder;
import com.smarthotel.exception.BusinessException;
import com.smarthotel.mapper.BookingGuestMapper;
import com.smarthotel.mapper.BookingOrderMapper;
import com.smarthotel.mapper.RoomTypeMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

class StaffOrderNoShowTest {
    private final BookingOrderMapper orders = mock(BookingOrderMapper.class);
    private final StaffOrderServiceImpl service = new StaffOrderServiceImpl(
            orders, mock(BookingGuestMapper.class), mock(RoomTypeMapper.class));

    @BeforeAll
    static void initializeEntityMapping() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new Configuration(), ""), BookingOrder.class);
    }

    @Test
    void futureOrRefundingOrderCannotBeMarkedNoShow() {
        BookingOrder order = eligibleOrder();
        order.setCheckInDate(LocalDate.now());
        when(orders.selectById(45L)).thenReturn(order);
        assertEquals(409, assertThrows(BusinessException.class,
                () -> service.markNoShow(45L)).getCode());

        order.setCheckInDate(LocalDate.now().minusDays(1));
        order.setPaymentStatus("REFUNDING");
        assertEquals(409, assertThrows(BusinessException.class,
                () -> service.markNoShow(45L)).getCode());
        verify(orders, never()).update(any(), any(Wrapper.class));
    }

    @Test
    void changedOrderIsNotOverwritten() {
        when(orders.selectById(45L)).thenReturn(eligibleOrder());
        when(orders.update(any(), any(Wrapper.class))).thenReturn(0);

        assertEquals(409, assertThrows(BusinessException.class,
                () -> service.markNoShow(45L)).getCode());
    }

    @Test
    void bulkUpdateRequiresConfirmedPaidOverdueAndSetsNoShow() {
        when(orders.update(any(), any(Wrapper.class))).thenReturn(3);

        assertEquals(3, service.markOverdueNoShows());
        ArgumentCaptor<Wrapper<BookingOrder>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(orders).update(isNull(), captor.capture());
        LambdaUpdateWrapper<?> update = (LambdaUpdateWrapper<?>) captor.getValue();
        String sql = update.getSqlSegment();
        String values = update.getParamNameValuePairs().values().toString();
        assertTrue(sql.contains("orderStatus"), sql);
        assertTrue(sql.contains("paymentStatus"), sql);
        assertTrue(sql.contains("checkInDate"), sql);
        assertTrue(values.contains("CONFIRMED"), values);
        assertTrue(values.contains("PAID"), values);
        assertTrue(values.contains("NO_SHOW"), values);
    }

    private BookingOrder eligibleOrder() {
        BookingOrder order = new BookingOrder();
        order.setOrderId(45L);
        order.setOrderStatus("CONFIRMED");
        order.setPaymentStatus("PAID");
        order.setCheckInDate(LocalDate.now().minusDays(1));
        return order;
    }
}
