package com.smarthotel.dto.checkin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CheckInRequest {

    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @NotEmpty(message = "请选择入住房间")
    private List<Long> roomIds;

    @DecimalMin(
            value = "0.00",
            message = "押金不能小于0"
    )
    private BigDecimal depositPerRoom =
            BigDecimal.ZERO;

    private String remark;
}