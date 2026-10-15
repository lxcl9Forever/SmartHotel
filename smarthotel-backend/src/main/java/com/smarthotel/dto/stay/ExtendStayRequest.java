package com.smarthotel.dto.stay;

import lombok.Data;

import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;

@Data
public class ExtendStayRequest {

    /**
     * 订单ID
     */
    @NotNull
    private Long orderId;


    /**
     * 新退房日期
     */
    @NotNull
    private LocalDate newCheckOutDate;

}
