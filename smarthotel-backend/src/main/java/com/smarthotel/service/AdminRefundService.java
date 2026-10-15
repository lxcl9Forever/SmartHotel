package com.smarthotel.service;

import com.smarthotel.vo.AdminRefundVO;
import com.smarthotel.vo.PageResult;

public interface AdminRefundService {
    PageResult<AdminRefundVO> listRefunds(String status, Integer page, Integer pageSize);

    void approve(Long refundId);

    void reject(Long refundId);
}
