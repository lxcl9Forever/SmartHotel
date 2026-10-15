package com.smarthotel.controller;

import com.smarthotel.dto.stay.ExtendStayRequest;
import com.smarthotel.annotation.OperationLog;
import com.smarthotel.service.StayExtensionService;
import com.smarthotel.vo.stay.ExtendStayPreviewVO;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customer/stay")
@RequiredArgsConstructor
public class CustomerStayController {


    private final StayExtensionService stayExtensionService;


    /**
     * 客户续住费用预览
     */
    @PostMapping("/preview")
    public ExtendStayPreviewVO preview(
            Authentication authentication,
            @Valid @RequestBody ExtendStayRequest request
    ){

        return stayExtensionService.previewForCustomer((Long) authentication.getCredentials(), request);
    }


    /**
     * 客户确认续住
     */
    @PostMapping("/extend")
    @OperationLog(value = "客户续住", module = "STAY")
    public ExtendStayPreviewVO extend(
            Authentication authentication,
            @Valid @RequestBody ExtendStayRequest request
    ){

        return stayExtensionService.extendForCustomer((Long) authentication.getCredentials(), request);
    }

}
