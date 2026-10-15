package com.smarthotel.vo.stay;


import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;


@Data
public class ExtendStayPreviewVO {


    /**
     * 订单ID
     */
    private Long orderId;



    /**
     * 原退房日期
     */
    private LocalDate oldCheckOutDate;



    /**
     * 新退房日期
     */
    private LocalDate newCheckOutDate;



    /**
     * 增加住宿晚数
     */
    private Integer extraNights;



    /**
     * 新增费用
     */
    private BigDecimal extraAmount;



    /**
     * 可用房量
     */
    private Integer availableRoom;



    /**
     * 是否库存充足
     */
    private Boolean available;

    /**
     * 是否成功
     */
    private Boolean success;
}