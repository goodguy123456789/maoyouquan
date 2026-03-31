package com.maoyouquan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("users")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String passwordHash;
    private String email;
    private String nickname;
    private String avatarUrl;
    private String role;  // USER | ADMIN
    private Integer isActive;
    private LocalDateTime createdAt;
}
