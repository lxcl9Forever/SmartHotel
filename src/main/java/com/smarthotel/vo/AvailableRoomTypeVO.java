package com.smarthotel.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AvailableRoomTypeVO {

    private Long roomTypeId;

    private String typeCode;

    private String typeName;

    private BigDecimal area;

    private String bedType;

    private Integer bedCount;

    private Integer maxGuests;

    // 整个入住区间内最少还剩多少间
    private Integer availableQty;

    // 住完整个入住区间，一间房的价格
    private BigDecimal pricePerRoom;

    // 按用户需要的房间数量计算后的总价
    private BigDecimal totalPrice;
}