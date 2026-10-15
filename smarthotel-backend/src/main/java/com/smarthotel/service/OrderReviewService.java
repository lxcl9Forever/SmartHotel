package com.smarthotel.service;

import com.smarthotel.dto.review.CreateReviewRequest;
import com.smarthotel.vo.OrderReviewVO;
import com.smarthotel.vo.PageResult;

public interface OrderReviewService {
    void create(Long orderId, Long userId, CreateReviewRequest request);
    OrderReviewVO getMine(Long orderId, Long userId);
    PageResult<OrderReviewVO> listForAdmin(String status, Integer page, Integer pageSize);
    void updateStatus(Long reviewId, String status);
}
