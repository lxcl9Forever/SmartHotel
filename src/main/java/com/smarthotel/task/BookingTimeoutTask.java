package com.smarthotel.task;

import com.smarthotel.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingTimeoutTask {

    private final BookingService bookingService;

    /**
     * 每分钟检查一次超时订单
     */
    @Scheduled(fixedDelay = 60000)
    public void closeExpiredBookings() {

        bookingService.closeExpiredBookings();
    }
}