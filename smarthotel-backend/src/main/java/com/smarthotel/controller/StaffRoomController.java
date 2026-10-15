package com.smarthotel.controller;

import com.smarthotel.common.result.Result;
import com.smarthotel.service.RoomStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.smarthotel.entity.Room;
import com.smarthotel.mapper.RoomMapper;
import com.smarthotel.dto.room.RoomStatusChangeRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/staff/rooms")
@RequiredArgsConstructor
public class StaffRoomController {

    private final RoomStatusService roomStatusService;
    private final RoomMapper roomMapper;

    /**
     * 开始清洁
     *
     * DIRTY → CLEANING
     */
    @PostMapping("/{roomId}/start-cleaning")
    public Result<Void> startCleaning(
            Authentication authentication,
            @PathVariable Long roomId) {

        roomStatusService.startCleaning(
                authentication.getName(),
                roomId
        );

        return Result.success();
    }


    /**
     * 清洁完成
     *
     * CLEANING → VACANT
     */
    @PostMapping("/{roomId}/finish-cleaning")
    public Result<Void> finishCleaning(
            Authentication authentication,
            @PathVariable Long roomId) {

        roomStatusService.finishCleaning(
                authentication.getName(),
                roomId
        );

        return Result.success();
    }

    @GetMapping
    public Result<List<Room>> listRooms(
            @RequestParam(required = false)
            String status,
            @RequestParam(required = false) String roomNo,
            @RequestParam(required = false) Integer floorNo,
            @RequestParam(required = false) Long roomTypeId) {

        return Result.success(
                roomMapper.selectList(
                        new com.baomidou.mybatisplus
                                .core.conditions.query
                                .LambdaQueryWrapper<Room>()
                                .eq(status != null && !status.isBlank(), Room::getStatus, status)
                                .like(roomNo != null && !roomNo.isBlank(), Room::getRoomNo, roomNo)
                                .eq(floorNo != null, Room::getFloorNo, floorNo)
                                .eq(roomTypeId != null, Room::getRoomTypeId, roomTypeId)
                                .orderByAsc(
                                        Room::getFloorNo
                                )
                                .orderByAsc(
                                        Room::getRoomNo
                                )
                )
        );
    }

    @PostMapping("/{roomId}/maintenance")
    public Result<Void> startMaintenance(
            @PathVariable Long roomId,
            @Valid
            @RequestBody
            RoomStatusChangeRequest request,
            Principal principal) {

        roomStatusService.startMaintenance(
                principal.getName(),
                roomId,
                request.getReason()
        );

        return Result.success();
    }

    @PostMapping("/{roomId}/out-of-service")
    public Result<Void> setOutOfService(
            @PathVariable Long roomId,
            @Valid
            @RequestBody
            RoomStatusChangeRequest request,
            Principal principal) {

        roomStatusService.setOutOfService(
                principal.getName(),
                roomId,
                request.getReason()
        );

        return Result.success();
    }

    @PostMapping("/{roomId}/restore")
    public Result<Void> restoreRoom(
            @PathVariable Long roomId,
            @Valid
            @RequestBody
            RoomStatusChangeRequest request,
            Principal principal) {

        roomStatusService.restoreRoom(
                principal.getName(),
                roomId,
                request.getReason()
        );

        return Result.success();
    }
}
