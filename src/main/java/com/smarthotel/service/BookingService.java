package com.smarthotel.service;

import com.smarthotel.dto.booking.CreateBookingRequest;
import com.smarthotel.vo.BookingOrderVO;

public interface BookingService {

    BookingOrderVO createBooking(
            String username,
            CreateBookingRequest request
    );
}