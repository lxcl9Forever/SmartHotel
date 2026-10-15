package com.smarthotel.controller;

import com.smarthotel.common.result.Result;
import com.smarthotel.dto.admin.AdminOrderQuery;
import com.smarthotel.service.AdminOrderService;
import com.smarthotel.vo.AdminOrderVO;
import com.smarthotel.vo.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.smarthotel.vo.AdminOrderDetailVO;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService adminOrderService;


    /**
     * 管理员分页查询订单
     *
     * 示例：
     *
     * /api/v1/admin/orders
     * /api/v1/admin/orders?page=1&pageSize=10
     * /api/v1/admin/orders?orderStatus=CONFIRMED
     * /api/v1/admin/orders?paymentStatus=PAID
     * /api/v1/admin/orders?orderNo=202610
     * /api/v1/admin/orders?checkInDate=2026-10-10
     */
    @GetMapping
    public Result<PageResult<AdminOrderVO>>
    listOrders(
            @ModelAttribute
            AdminOrderQuery query
    ) {

        return Result.success(
                adminOrderService.listOrders(query)
        );
    }

    /**
     * 查询订单详情
     */
    @GetMapping("/{orderId}")
    public Result<AdminOrderDetailVO>
    getOrderDetail(
            @PathVariable
            Long orderId
    ) {

        return Result.success(
                adminOrderService.getOrderDetail(
                        orderId
                )
        );
    }
}