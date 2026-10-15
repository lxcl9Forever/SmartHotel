package com.smarthotel.service;

import com.smarthotel.dto.admin.AdminOrderQuery;
import com.smarthotel.vo.AdminOrderVO;
import com.smarthotel.vo.PageResult;
import com.smarthotel.vo.AdminOrderDetailVO;

public interface AdminOrderService {

    /**
     * 管理员分页查询订单
     */
    PageResult<AdminOrderVO> listOrders(
            AdminOrderQuery query
    );

    /**
     * 查询管理员订单详情
     */
    AdminOrderDetailVO getOrderDetail(
            Long orderId
    );
}