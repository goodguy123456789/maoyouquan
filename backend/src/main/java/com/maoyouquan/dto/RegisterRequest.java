package com.maoyouquan.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank @Size(min = 3, max = 50) private String username;
    @NotBlank @Size(min = 6) private String password;
    @Email private String email;
    private String nickname;
}
