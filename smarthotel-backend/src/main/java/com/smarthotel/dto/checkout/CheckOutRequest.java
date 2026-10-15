package com.smarthotel.dto.checkout;

import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class CheckOutRequest {

    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    private String remark;
}
