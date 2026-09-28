package com.smarthotel.service;

import com.smarthotel.dto.booking.CreateBookingRequest;
import com.smarthotel.vo.BookingOrderVO;

import java.util.List;

public interface BookingService {

    /**
     * 创建订单
     */
    BookingOrderVO createBooking(
            String username,
            CreateBookingRequest request
    );

    /**
     * 查询当前客户的全部订单
     */
    List<BookingOrderVO> listMyBookings(
            String username
    );


    /**
     * 查询当前客户的某个订单详情
     */
    BookingOrderVO getMyBooking(
            String username,
            Long orderId
    );

    void cancelMyBooking(
            String username,
            Long orderId
    );

    void closeExpiredBookings();

    BookingOrderVO mockPaySuccess(
            String username,
            Long orderId
    );

}