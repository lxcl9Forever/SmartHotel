package com.smarthotel.controller;

import com.smarthotel.annotation.OperationLog;
import com.smarthotel.service.RefundService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/customer/refund")
@RequiredArgsConstructor
public class RefundController {


    private final RefundService refundService;


    @PostMapping("/apply")
    @OperationLog(value = "申请退款", module = "REFUND")
    public String apply(
            @RequestParam Long orderId,
            @RequestParam String reason,
            Authentication authentication
    ){

        Long userId =
                (Long) authentication.getCredentials();


        refundService.applyRefund(
                orderId,
                userId,
                reason
        );


        return "退款申请成功";

    }

}
