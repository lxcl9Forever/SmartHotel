package com.smarthotel.vo;

import lombok.Data;

@Data
public class AvailablePhysicalRoomVO {

    private Long roomId;

    private String roomNo;

    private Integer floorNo;

    private String status;
}