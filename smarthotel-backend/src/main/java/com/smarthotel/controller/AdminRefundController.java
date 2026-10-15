package com.smarthotel.controller;

import com.smarthotel.annotation.OperationLog;
import com.smarthotel.common.result.Result;
import com.smarthotel.service.AdminRefundService;
import com.smarthotel.service.RefundInventorySyncService;
import com.smarthotel.vo.AdminRefundVO;
import com.smarthotel.vo.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/refund")
@RequiredArgsConstructor
public class AdminRefundController {

    private final AdminRefundService adminRefundService;
    private final RefundInventorySyncService refundInventorySyncService;

    @GetMapping("/list")
    public Result<PageResult<AdminRefundVO>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        return Result.success(adminRefundService.listRefunds(status, page, pageSize));
    }

    @PostMapping("/{refundId}/approve")
    @OperationLog(value = "同意退款", module = "REFUND")
    public Result<Void> approve(@PathVariable Long refundId) {
        adminRefundService.approve(refundId);
        refundInventorySyncService.syncOne(refundId);
        return Result.success();
    }

    @PostMapping("/{refundId}/reject")
    @OperationLog(value = "拒绝退款", module = "REFUND")
    public Result<Void> reject(@PathVariable Long refundId) {
        adminRefundService.reject(refundId);
        return Result.success();
    }
}
