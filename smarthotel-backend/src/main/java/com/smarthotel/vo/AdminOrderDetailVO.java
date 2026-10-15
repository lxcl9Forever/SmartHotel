package com.smarthotel.vo;

import com.smarthotel.entity.BookingGuest;
import lombok.Data;

import java.util.List;

@Data
public class AdminOrderDetailVO {

    /**
     * 订单基本信息
     */
    private AdminOrderVO order;

    /**
     * 入住人信息
     */
    private List<BookingGuest> guests;
}