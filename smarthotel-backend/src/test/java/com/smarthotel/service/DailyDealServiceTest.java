package com.smarthotel.service;

import com.smarthotel.dto.booking.CreateBookingRequest;
import com.smarthotel.entity.DailyDeal;
import com.smarthotel.exception.BusinessException;
import com.smarthotel.mapper.BookingOrderMapper;
import com.smarthotel.mapper.DailyDealMapper;
import com.smarthotel.mapper.RoomInventoryMapper;
import com.smarthotel.mapper.RoomPriceMapper;
import com.smarthotel.mapper.RoomTypeMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DailyDealServiceTest {
    private final DailyDealMapper deals = mock(DailyDealMapper.class);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final DailyDealService service = new DailyDealService(deals, mock(BookingOrderMapper.class),
            mock(RoomTypeMapper.class), mock(RoomPriceMapper.class), mock(RoomInventoryMapper.class), jdbc);

    private DailyDeal activeDeal() {
        DailyDeal deal = new DailyDeal();
        deal.setId(7L);
        deal.setRoomTypeId(3L);
        deal.setCheckInDate(LocalDate.now().plusDays(2));
        deal.setCheckOutDate(LocalDate.now().plusDays(3));
        deal.setStartTime(LocalDateTime.now().minusMinutes(1));
        deal.setEndTime(LocalDateTime.now().plusMinutes(20));
        deal.setQuota(2);
        deal.setDiscountRate(new BigDecimal("0.5000"));
        deal.setStatus("ACTIVE");
        return deal;
    }

    private CreateBookingRequest request(DailyDeal deal) {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setDailyDealId(deal.getId());
        request.setRoomTypeId(deal.getRoomTypeId());
        request.setCheckInDate(deal.getCheckInDate());
        request.setCheckOutDate(deal.getCheckOutDate());
        request.setRoomCount(1);
        return request;
    }

    @Test
    void usesOnlyDealPriceAndRejectsCoupon() {
        DailyDeal deal = activeDeal();
        PromotionService.Price price = service.price(deal, new BigDecimal("688.00"));
        assertNull(price.promotionId());
        assertEquals(new BigDecimal("344.00"), price.payableAmount());

        when(deals.selectOne(any())).thenReturn(deal);
        CreateBookingRequest request = request(deal);
        request.setCouponId(4L);
        assertEquals("秒杀订单不能使用优惠券",
                assertThrows(BusinessException.class, () -> service.lockForBooking(request)).getMessage());
        verifyNoInteractions(jdbc);
    }

    @Test
    void rejectsLastRoomWhenRequestedQuantityExceedsRemainingQuota() {
        DailyDeal deal = activeDeal();
        when(deals.selectOne(any())).thenReturn(deal);
        when(jdbc.queryForList(anyString(), eq(Integer.class), eq(7L))).thenReturn(List.of(1, 1));
        assertEquals("秒杀名额已抢完",
                assertThrows(BusinessException.class, () -> service.lockForBooking(request(deal))).getMessage());
    }
}
