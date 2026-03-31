package com.maoyouquan.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NewsRequest {
    @NotBlank private String title;
    @NotBlank private String content;
    @NotBlank private String category;
    private String coverUrl;
}
