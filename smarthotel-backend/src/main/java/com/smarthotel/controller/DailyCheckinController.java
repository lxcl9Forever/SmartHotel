package com.smarthotel.controller;

import com.smarthotel.annotation.OperationLog;
import com.smarthotel.common.result.Result;
import com.smarthotel.service.DailyCheckinService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class DailyCheckinController {
    private final DailyCheckinService service;

    public record RewardUpdate(@NotNull BigDecimal amount) {}

    @GetMapping("/api/v1/customer/checkin")
    public Result<DailyCheckinService.Overview> overview(Authentication auth) {
        return Result.success(service.overview(auth.getName()));
    }

    @PostMapping("/api/v1/customer/checkin")
    @OperationLog(value = "每日签到领取优惠券", module = "CHECKIN")
    public Result<DailyCheckinService.Overview> checkIn(Authentication auth) {
        return Result.success(service.checkIn(auth.getName()));
    }

    @GetMapping("/api/v1/admin/checkin/rewards")
    public Result<List<DailyCheckinService.Reward>> rewards() {
        return Result.success(service.rewards());
    }

    @PutMapping("/api/v1/admin/checkin/rewards/{day}")
    @OperationLog(value = "修改每日签到奖励", module = "CHECKIN")
    public Result<Void> update(@PathVariable int day, @Valid @RequestBody RewardUpdate request) {
        service.updateReward(day, request.amount());
        return Result.success();
    }
}
