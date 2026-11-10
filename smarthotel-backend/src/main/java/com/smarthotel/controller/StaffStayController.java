package com.smarthotel.controller;

import com.smarthotel.dto.stay.ExtendStayRequest;
import com.smarthotel.service.StayExtensionService;
import com.smarthotel.vo.stay.ExtendStayPreviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/staff/stay")
@RequiredArgsConstructor
public class StaffStayController {

    private final StayExtensionService stayExtensionService;

    /**
     * 续住费用预览
     *
     * STAFF查看：
     * 1. 新增几晚
     * 2. 新增费用
     * 3. 是否有库存
     */
    @PostMapping("/preview")
    public ExtendStayPreviewVO preview(
            @RequestBody ExtendStayRequest request
    ){

        return stayExtensionService.preview(request);

    }

    /**
     * 确认续住
     *
     * 真正修改：
     * booking_order
     * room_inventory
     * redis
     * payment
     */
    @PostMapping("/extend")
    public String extend(
            @RequestBody ExtendStayRequest request
    ){

        stayExtensionService.extend(request);

        return "续住成功";

    }

}