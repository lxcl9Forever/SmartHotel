package com.smarthotel.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 50)
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "请输入验证码")
    @Size(max = 36)
    private String captchaId;

    @NotBlank(message = "请输入验证码")
    @Size(max = 5)
    private String captchaCode;
}
