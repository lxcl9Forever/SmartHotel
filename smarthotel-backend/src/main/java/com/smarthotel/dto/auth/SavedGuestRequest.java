package com.smarthotel.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SavedGuestRequest(
        @NotBlank(message = "入住人姓名不能为空") @Size(max = 50) String guestName,
        @NotBlank(message = "手机号不能为空") @Pattern(regexp = "^1\\d{10}$", message = "请输入正确的11位手机号") String phone,
        @NotBlank @Pattern(regexp = "ID_CARD|PASSPORT|OTHER", message = "证件类型不正确") String idType,
        @Size(max = 50) String idNumber) {
}
