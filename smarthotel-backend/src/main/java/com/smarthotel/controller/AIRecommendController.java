package com.smarthotel.controller;

import com.smarthotel.common.result.Result;
import com.smarthotel.dto.ai.RecommendRequest;
import com.smarthotel.service.AIRecommendService;
import com.smarthotel.vo.AIRecommendationVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customer/ai")
@RequiredArgsConstructor
public class AIRecommendController {
    private final AIRecommendService service;

    @PostMapping("/recommend")
    public Result<AIRecommendationVO> recommend(@Valid @RequestBody RecommendRequest request) {
        return Result.success(service.recommend(request));
    }
}
