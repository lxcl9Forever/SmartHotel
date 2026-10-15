package com.smarthotel.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class StaffStayHistoryVO {
    private Long checkInId;
    private String orderNo;
    private String contactName;
    private String roomNo;
    private Integer floorNo;
    private String roomTypeName;
    private LocalDateTime actualCheckInTime;
    private LocalDateTime actualCheckOutTime;
    private String checkInStaff;
    private String checkOutStaff;
}
