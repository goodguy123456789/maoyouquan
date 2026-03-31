package com.maoyouquan.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CatSubmitRequest {
    @NotBlank private String name;
    private String gender;
    private Integer age;
    private String breed;
    private String avatarUrl;
    private String personality;
}
