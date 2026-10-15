package com.smarthotel.controller;

import com.smarthotel.common.result.Result;
import com.smarthotel.dto.checkin.CheckInRequest;
import com.smarthotel.service.CheckInService;
import com.smarthotel.vo.AvailablePhysicalRoomVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/staff/check-ins")
@RequiredArgsConstructor
public class StaffCheckInController {

    private final CheckInService checkInService;


    /**
     * 查询某订单可分配的实体客房
     */
    @GetMapping(
            "/orders/{orderId}/available-rooms"
    )
    public Result<List<AvailablePhysicalRoomVO>>
    listAvailableRooms(
            @PathVariable Long orderId) {

        return Result.success(
                checkInService.listAvailableRooms(
                        orderId
                )
        );
    }


    /**
     * 办理入住
     */
    @PostMapping
    public Result<Void> checkIn(
            Authentication authentication,
            @Valid @RequestBody
            CheckInRequest request) {

        checkInService.checkIn(
                authentication.getName(),
                request
        );

        return Result.success();
    }
}