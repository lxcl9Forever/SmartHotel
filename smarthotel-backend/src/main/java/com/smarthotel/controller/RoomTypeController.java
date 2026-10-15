package com.smarthotel.controller;

import com.smarthotel.common.result.Result;
import com.smarthotel.entity.RoomType;
import com.smarthotel.service.RoomTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.smarthotel.vo.AvailableRoomTypeVO;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/room-types")
@RequiredArgsConstructor
public class RoomTypeController {

    private final RoomTypeService roomTypeService;

    @GetMapping
    public Result<List<RoomType>> listRoomTypes() {

        return Result.success(
                roomTypeService.listAvailableRoomTypes()
        );
    }

    @GetMapping("/{roomTypeId}")
    public Result<RoomType> getRoomType(
            @PathVariable Long roomTypeId) {

        return Result.success(
                roomTypeService.getRoomTypeById(roomTypeId)
        );
    }

    @GetMapping("/available")
    public Result<List<AvailableRoomTypeVO>> searchAvailableRoomTypes(
            @RequestParam LocalDate checkInDate,
            @RequestParam LocalDate checkOutDate,
            @RequestParam Integer roomCount,
            @RequestParam Integer guestCount) {

        return Result.success(
                roomTypeService.searchAvailableRoomTypes(
                        checkInDate,
                        checkOutDate,
                        roomCount,
                        guestCount
                )
        );
    }
}