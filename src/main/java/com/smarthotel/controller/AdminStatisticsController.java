package com.smarthotel.controller;

import com.smarthotel.common.result.Result;
import com.smarthotel.entity.HotelDailyStatistics;
import com.smarthotel.service.AdminStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/statistics")
@RequiredArgsConstructor
public class AdminStatisticsController {

    private final AdminStatisticsService
            adminStatisticsService;


    /*
     * 手动重新生成某一天统计
     */
    @PostMapping("/refresh")
    public Result<Void> refresh(
            @RequestParam LocalDate date) {

        adminStatisticsService
                .refreshDailyStatistics(
                        date
                );

        return Result.success();
    }


    /*
     * 查询日期区间统计
     */
    @GetMapping("/daily")
    public Result<List<HotelDailyStatistics>>
    listDailyStatistics(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {

        return Result.success(
                adminStatisticsService
                        .listDailyStatistics(
                                startDate,
                                endDate
                        )
        );
    }

    @PostMapping("/refresh-range")
    public Result<Void> refreshRange(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {

        adminStatisticsService
                .refreshStatisticsRange(
                        startDate,
                        endDate
                );

        return Result.success();
    }

}