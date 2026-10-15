package com.smarthotel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smarthotel.common.result.Result;
import com.smarthotel.entity.BookingOrder;
import com.smarthotel.entity.CheckInRecord;
import com.smarthotel.entity.CheckOutRecord;
import com.smarthotel.entity.Room;
import com.smarthotel.mapper.BookingOrderMapper;
import com.smarthotel.mapper.CheckInRecordMapper;
import com.smarthotel.mapper.CheckOutRecordMapper;
import com.smarthotel.mapper.RoomMapper;
import com.smarthotel.mapper.StaffStayHistoryMapper;
import com.smarthotel.vo.PageResult;
import com.smarthotel.vo.StaffStayHistoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/staff/workbench")
@RequiredArgsConstructor
public class StaffWorkbenchController {
    private final StaffStayHistoryMapper historyMapper;
    private final BookingOrderMapper orderMapper;
    private final CheckInRecordMapper checkInMapper;
    private final CheckOutRecordMapper checkOutMapper;
    private final RoomMapper roomMapper;

    @GetMapping("/history")
    public Result<PageResult<StaffStayHistoryVO>> history(@RequestParam(defaultValue = "1") Integer page,
                                                            @RequestParam(defaultValue = "10") Integer pageSize) {
        int p = Math.max(1, page);
        int size = Math.max(1, Math.min(pageSize, 100));
        long total = historyMapper.countAll();
        return Result.success(new PageResult<>(total, p, size,
                historyMapper.list((long) (p - 1) * size, size)));
    }

    @GetMapping("/stats")
    public Result<Map<String, Long>> stats() {
        LocalDate today = LocalDate.now();
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("pendingCheckIn", orderMapper.selectCount(new LambdaQueryWrapper<BookingOrder>()
                .eq(BookingOrder::getOrderStatus, "CONFIRMED")));
        stats.put("occupied", roomMapper.selectCount(new LambdaQueryWrapper<Room>().eq(Room::getStatus, "OCCUPIED")));
        stats.put("dirty", roomMapper.selectCount(new LambdaQueryWrapper<Room>().eq(Room::getStatus, "DIRTY")));
        stats.put("todayCheckIns", checkInMapper.selectCount(new LambdaQueryWrapper<CheckInRecord>()
                .ge(CheckInRecord::getActualCheckInTime, today.atStartOfDay())
                .lt(CheckInRecord::getActualCheckInTime, today.plusDays(1).atStartOfDay())));
        stats.put("todayCheckOuts", checkOutMapper.selectCount(new LambdaQueryWrapper<CheckOutRecord>()
                .ge(CheckOutRecord::getActualCheckOutTime, today.atStartOfDay())
                .lt(CheckOutRecord::getActualCheckOutTime, today.plusDays(1).atStartOfDay())));
        return Result.success(stats);
    }
}
