package com.smarthotel.controller;

import com.smarthotel.common.result.Result;
import com.smarthotel.dto.booking.CreateBookingRequest;
import com.smarthotel.service.BookingService;
import com.smarthotel.vo.BookingOrderVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public Result<BookingOrderVO> createBooking(
            Authentication authentication,
            @Valid @RequestBody CreateBookingRequest request) {

        return Result.success(
                bookingService.createBooking(
                        authentication.getName(),
                        request
                )
        );
    }
}