package com.smarthotel.dto.room;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RoomStatusChangeRequest {

    @NotBlank(message = "变更原因不能为空")
    private String reason;
}