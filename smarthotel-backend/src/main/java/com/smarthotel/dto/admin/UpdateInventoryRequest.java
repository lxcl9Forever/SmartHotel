package com.smarthotel.dto.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateInventoryRequest {

    @NotNull(message = "房型ID不能为空")
    private Long roomTypeId;

    @NotNull(message = "库存日期不能为空")
    private LocalDate inventoryDate;

    @NotNull(message = "总房量不能为空")
    @Min(
            value = 0,
            message = "总房量不能小于0"
    )
    private Integer totalQty;
}