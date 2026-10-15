package com.smarthotel.controller;

import com.smarthotel.annotation.OperationLog;
import com.smarthotel.common.result.Result;
import com.smarthotel.dto.promotion.PromotionRequest;
import com.smarthotel.entity.Promotion;
import com.smarthotel.service.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/promotion")
@RequiredArgsConstructor
public class PromotionController {
    private final PromotionService service;

    @PostMapping
    @OperationLog(value = "创建优惠活动", module = "PROMOTION")
    public Result<Promotion> create(@Valid @RequestBody PromotionRequest request) {
        return Result.success(service.create(request));
    }

    @GetMapping("/list")
    public Result<List<Promotion>> list() {
        return Result.success(service.list());
    }

    @PutMapping("/{id}")
    @OperationLog(value = "修改优惠活动", module = "PROMOTION")
    public Result<Promotion> update(@PathVariable Long id,
                                    @Valid @RequestBody PromotionRequest request) {
        return Result.success(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @OperationLog(value = "删除优惠活动", module = "PROMOTION")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.success();
    }

    @PutMapping("/{id}/enabled")
    @OperationLog(value = "开关优惠活动", module = "PROMOTION")
    public Result<Void> setEnabled(@PathVariable Long id, @RequestParam boolean enabled) {
        service.setEnabled(id, enabled);
        return Result.success();
    }
}
