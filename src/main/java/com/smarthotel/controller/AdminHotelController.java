package com.smarthotel.controller;

import com.smarthotel.common.result.Result;
import com.smarthotel.entity.Room;
import com.smarthotel.entity.RoomType;
import com.smarthotel.service.AdminHotelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.smarthotel.entity.RoomInventory;
import com.smarthotel.entity.RoomPrice;

import java.time.LocalDate;
import com.smarthotel.dto.admin.UpdateInventoryRequest;
import com.smarthotel.dto.admin.UpdatePriceRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminHotelController {

    private final AdminHotelService adminHotelService;


    /**
     * 查询全部房型
     */
    @GetMapping("/room-types")
    public Result<List<RoomType>>
    listRoomTypes() {

        return Result.success(
                adminHotelService.listRoomTypes()
        );
    }


    /**
     * 查询实体客房
     *
     * 示例：
     *
     * /rooms
     * /rooms?roomTypeId=1
     * /rooms?status=VACANT
     * /rooms?roomTypeId=1&status=VACANT
     */
    @GetMapping("/rooms")
    public Result<List<Room>>
    listRooms(
            @RequestParam(required = false)
            Long roomTypeId,

            @RequestParam(required = false)
            String status) {

        return Result.success(
                adminHotelService.listRooms(
                        roomTypeId,
                        status
                )
        );
    }

    @GetMapping("/inventories")
    public Result<List<RoomInventory>>
    listInventories(
            @RequestParam Long roomTypeId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {

        return Result.success(
                adminHotelService.listInventories(
                        roomTypeId,
                        startDate,
                        endDate
                )
        );
    }


    @GetMapping("/prices")
    public Result<List<RoomPrice>>
    listPrices(
            @RequestParam Long roomTypeId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {

        return Result.success(
                adminHotelService.listPrices(
                        roomTypeId,
                        startDate,
                        endDate
                )
        );
    }

    @PutMapping("/inventories")
    public Result<Void> updateInventory(
            @Valid
            @RequestBody
            UpdateInventoryRequest request) {

        adminHotelService.updateInventory(
                request
        );

        return Result.success();
    }


    @PutMapping("/prices")
    public Result<Void> updatePrice(
            @Valid
            @RequestBody
            UpdatePriceRequest request) {

        adminHotelService.updatePrice(
                request
        );

        return Result.success();
    }

}