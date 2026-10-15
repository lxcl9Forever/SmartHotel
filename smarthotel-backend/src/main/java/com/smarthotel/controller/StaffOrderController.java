package com.smarthotel.controller;

import com.smarthotel.common.result.Result;
import com.smarthotel.annotation.OperationLog;
import com.smarthotel.service.StaffOrderService;
import com.smarthotel.vo.StaffOrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.smarthotel.vo.BookingGuestVO;

import java.util.List;

@RestController
@RequestMapping("/api/v1/staff/orders")
@RequiredArgsConstructor
public class StaffOrderController {

    private final StaffOrderService staffOrderService;


    @GetMapping
    public Result<List<StaffOrderVO>> listOrders(
            @RequestParam(required = false)
            String status) {

        return Result.success(
                staffOrderService.listOrders(
                        status
                )
        );
    }

    @GetMapping("/{orderId}/guests")
    public Result<List<BookingGuestVO>>
    listOrderGuests(
            @PathVariable Long orderId) {

        return Result.success(
                staffOrderService
                        .listOrderGuests(
                                orderId
                        )
        );
    }

    @PostMapping("/{orderId}/complete")
    public Result<Void> completeOrder(
            @PathVariable Long orderId
    ){

        staffOrderService.completeOrder(orderId);

        return Result.success();

    }

    @PostMapping("/{orderId}/no-show")
    @OperationLog(value = "标记未入住", module = "BOOKING")
    public Result<Void> markNoShow(@PathVariable Long orderId) {
        staffOrderService.markNoShow(orderId);
        return Result.success();
    }

    @PostMapping("/overdue/no-show")
    @OperationLog(value = "批量标记未入住", module = "BOOKING")
    public Result<Integer> markOverdueNoShows() {
        return Result.success(staffOrderService.markOverdueNoShows());
    }
}
