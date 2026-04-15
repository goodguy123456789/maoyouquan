package com.maoyouquan.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "用户名不能为空") @Size(min = 3, max = 50, message = "用户名长度为3-50个字符") private String username;
    @NotBlank(message = "密码不能为空") @Size(min = 6, message = "密码至少6位") private String password;
    @NotBlank(message = "请再次输入密码") private String confirmPassword;
    @NotBlank(message = "邮箱不能为空") @Email(message = "邮箱格式不正确") private String email;
    private String nickname;

    @AssertTrue(message = "两次输入的密码不一致")
    public boolean isPasswordMatched() {
        return password != null && password.equals(confirmPassword);
    }
}
