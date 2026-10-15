package com.smarthotel.dto.checkin;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CheckInRequest {

    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @NotEmpty(message = "请选择入住房间")
    private List<Long> roomIds;

    private String remark;
}
