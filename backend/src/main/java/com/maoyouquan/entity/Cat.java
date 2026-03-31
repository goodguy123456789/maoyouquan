package com.maoyouquan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("cats")
public class Cat {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String gender;   // MALE | FEMALE
    private Integer age;
    private String breed;
    private String avatarUrl;
    private String avatarPath;
    private String personality;
    private Integer likeCount;
    private String status;   // PENDING | APPROVED | REJECTED
    private Long submitterId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
