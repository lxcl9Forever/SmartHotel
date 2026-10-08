package com.smarthotel.dto.booking;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BookingGuestRequest {

    @NotBlank(message = "入住人姓名不能为空")
    private String guestName;

    private String phone;

    private String idType = "ID_CARD";

    private String idNumber;
}