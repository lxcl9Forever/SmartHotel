package com.smarthotel.controller;

import com.smarthotel.common.result.Result;
import com.smarthotel.dto.review.CreateReviewRequest;
import com.smarthotel.service.OrderReviewService;
import com.smarthotel.vo.OrderReviewVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customer/reviews")
@RequiredArgsConstructor
public class CustomerReviewController {
    private final OrderReviewService reviewService;

    @PostMapping("/orders/{orderId}")
    public Result<Void> create(@PathVariable Long orderId, @Valid @RequestBody CreateReviewRequest request,
                               Authentication authentication) {
        reviewService.create(orderId, (Long) authentication.getCredentials(), request);
        return Result.success();
    }

    @GetMapping("/orders/{orderId}")
    public Result<OrderReviewVO> mine(@PathVariable Long orderId, Authentication authentication) {
        return Result.success(reviewService.getMine(orderId, (Long) authentication.getCredentials()));
    }
}
