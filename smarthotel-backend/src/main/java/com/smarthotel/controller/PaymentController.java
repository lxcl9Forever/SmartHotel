package com.smarthotel.controller;

import com.smarthotel.annotation.OperationLog;
import com.smarthotel.common.result.Result;
import com.smarthotel.service.AlipayService;
import com.smarthotel.vo.AlipayPageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {
    private final AlipayService alipayService;

    @PostMapping("/create")
    @OperationLog(value = "发起支付宝支付", module = "PAYMENT")
    public Result<AlipayPageVO> create(Authentication authentication,
                                       @RequestParam Long orderId) {
        return Result.success(alipayService.createPage(authentication.getName(), orderId));
    }

    @PostMapping("/query")
    @OperationLog(value = "查询支付宝支付结果", module = "PAYMENT")
    public Result<String> query(Authentication authentication, @RequestParam Long orderId) {
        return Result.success(alipayService.queryAndConfirm(authentication.getName(), orderId));
    }

    @PostMapping(value = "/notify", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE)
    @OperationLog(value = "支付宝支付回调", module = "PAYMENT")
    public String notify(@RequestParam Map<String, String> params) {
        return alipayService.handleNotify(params) ? "success" : "failure";
    }
}
