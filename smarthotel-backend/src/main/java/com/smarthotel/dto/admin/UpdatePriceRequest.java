package com.smarthotel.dto.admin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdatePriceRequest {

    @NotNull(message = "房型ID不能为空")
    private Long roomTypeId;

    @NotNull(message = "房价日期不能为空")
    private LocalDate priceDate;

    @NotNull(message = "售价不能为空")
    @DecimalMin(
            value = "0.01",
            message = "售价必须大于0"
    )
    private BigDecimal salePrice;
}