package com.smarthotel.controller;

import com.smarthotel.common.result.Result;
import com.smarthotel.dto.booking.CreateBookingRequest;
import com.smarthotel.service.BookingService;
import com.smarthotel.vo.BookingOrderVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;


    /**
     * 创建订单
     */
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


    /**
     * 我的订单
     */
    @GetMapping("/my")
    public Result<List<BookingOrderVO>> listMyBookings(
            Authentication authentication) {

        return Result.success(
                bookingService.listMyBookings(
                        authentication.getName()
                )
        );
    }


    /**
     * 我的订单详情
     */
    @GetMapping("/{orderId}")
    public Result<BookingOrderVO> getMyBooking(
            Authentication authentication,
            @PathVariable Long orderId) {

        return Result.success(
                bookingService.getMyBooking(
                        authentication.getName(),
                        orderId
                )
        );
    }

    /**
     * 取消自己的待支付订单
     */
    @PostMapping("/{orderId}/cancel")
    public Result<Void> cancelBooking(
            Authentication authentication,
            @PathVariable Long orderId) {

        bookingService.cancelMyBooking(
                authentication.getName(),
                orderId
        );

        return Result.success();
    }

    /**
     * 开发阶段模拟支付成功
     */
    @PostMapping("/{orderId}/mock-pay")
    public Result<BookingOrderVO> mockPay(
            Authentication authentication,
            @PathVariable Long orderId) {

        return Result.success(
                bookingService.mockPaySuccess(
                        authentication.getName(),
                        orderId
                )
        );
    }
}