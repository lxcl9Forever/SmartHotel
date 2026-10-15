package com.smarthotel.controller;

import com.smarthotel.annotation.OperationLog;
import com.smarthotel.common.result.Result;
import com.smarthotel.service.OrderReviewService;
import com.smarthotel.vo.OrderReviewVO;
import com.smarthotel.vo.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {
    private final OrderReviewService reviewService;

    @GetMapping
    public Result<PageResult<OrderReviewVO>> list(@RequestParam(required = false) String status,
                                                   @RequestParam(defaultValue = "1") Integer page,
                                                   @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(reviewService.listForAdmin(status, page, pageSize));
    }

    @PostMapping("/{reviewId}/hide")
    @OperationLog(value = "隐藏评价", module = "REVIEW")
    public Result<Void> hide(@PathVariable Long reviewId) {
        reviewService.updateStatus(reviewId, "HIDDEN");
        return Result.success();
    }

    @PostMapping("/{reviewId}/show")
    @OperationLog(value = "展示评价", module = "REVIEW")
    public Result<Void> show(@PathVariable Long reviewId) {
        reviewService.updateStatus(reviewId, "VISIBLE");
        return Result.success();
    }
}
