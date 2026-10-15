package com.smarthotel.controller;

import com.smarthotel.annotation.OperationLog;
import com.smarthotel.common.result.Result;
import com.smarthotel.dto.promotion.DailyDealRequest;
import com.smarthotel.entity.DailyDeal;
import com.smarthotel.service.DailyDealService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class DailyDealController {
    private final DailyDealService service;

    @GetMapping("/api/v1/daily-deals")
    public Result<List<DailyDealService.Offer>> publicOffers() {
        return Result.success(service.publicOffers());
    }

    @GetMapping("/api/v1/admin/daily-deals")
    public Result<List<DailyDeal>> adminList() {
        return Result.success(service.adminList());
    }

    @PostMapping("/api/v1/admin/daily-deals")
    @OperationLog(value = "创建每日优惠秒杀", module = "PROMOTION")
    public Result<DailyDeal> create(@Valid @RequestBody DailyDealRequest request) {
        return Result.success(service.create(request));
    }

    @PutMapping("/api/v1/admin/daily-deals/{id}")
    @OperationLog(value = "修改每日优惠秒杀", module = "PROMOTION")
    public Result<DailyDeal> update(@PathVariable Long id, @Valid @RequestBody DailyDealRequest request) {
        return Result.success(service.update(id, request));
    }

    @PutMapping("/api/v1/admin/daily-deals/{id}/enabled")
    @OperationLog(value = "开关每日优惠秒杀", module = "PROMOTION")
    public Result<Void> setEnabled(@PathVariable Long id, @RequestParam boolean enabled) {
        service.setEnabled(id, enabled);
        return Result.success();
    }

    @DeleteMapping("/api/v1/admin/daily-deals/{id}")
    @OperationLog(value = "删除每日优惠秒杀", module = "PROMOTION")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.success();
    }
}
