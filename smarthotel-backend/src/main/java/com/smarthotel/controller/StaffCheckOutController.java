package com.smarthotel.controller;

import com.smarthotel.common.result.Result;
import com.smarthotel.dto.checkout.CheckOutRequest;
import com.smarthotel.service.CheckOutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/staff/check-outs")
@RequiredArgsConstructor
public class StaffCheckOutController {

    private final CheckOutService checkOutService;


    @PostMapping
    public Result<Void> checkOut(
            Authentication authentication,
            @Valid @RequestBody
            CheckOutRequest request) {

        checkOutService.checkOut(
                authentication.getName(),
                request
        );

        return Result.success();
    }
}