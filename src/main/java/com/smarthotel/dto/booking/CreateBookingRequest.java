package com.smarthotel.dto.booking;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.time.LocalDate;

@Data
public class CreateBookingRequest {

    @NotNull(message = "房型不能为空")
    private Long roomTypeId;

    @NotNull(message = "房间数量不能为空")
    @Min(value = 1, message = "房间数量至少为1")
    private Integer roomCount;

    @NotNull(message = "入住人数不能为空")
    @Min(value = 1, message = "入住人数至少为1")
    private Integer guestCount;

    @NotNull(message = "入住日期不能为空")
    @FutureOrPresent(message = "入住日期不能早于今天")
    private LocalDate checkInDate;

    @NotNull(message = "退房日期不能为空")
    private LocalDate checkOutDate;

    @NotBlank(message = "联系人姓名不能为空")
    private String contactName;

    @NotBlank(message = "联系人手机号不能为空")
    private String contactPhone;

    private String remark;

    @Valid
    @NotEmpty(message = "入住人信息不能为空")
    private List<BookingGuestRequest> guests;
}