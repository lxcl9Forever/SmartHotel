package com.smarthotel.dto.checkout;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CheckOutRequest {

    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @DecimalMin(
            value = "0.00",
            message = "额外消费不能小于0"
    )
    private BigDecimal extraChargePerRoom =
            BigDecimal.ZERO;

    private String remark;
}