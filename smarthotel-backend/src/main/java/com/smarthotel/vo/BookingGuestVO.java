package com.smarthotel.vo;

import lombok.Data;

@Data
public class BookingGuestVO {

    private Long guestId;

    private String guestName;

    private String phone;

    private String idType;

    private String idNumber;

    private Integer isPrimary;
}