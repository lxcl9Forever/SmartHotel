package com.smarthotel.service;

import com.smarthotel.vo.StaffOrderVO;
import com.smarthotel.vo.BookingGuestVO;

import java.util.List;

public interface StaffOrderService {

    List<StaffOrderVO> listOrders(
            String status
    );

    List<BookingGuestVO> listOrderGuests(
            Long orderId
    );

}