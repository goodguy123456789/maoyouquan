package com.maoyouquan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("comments")
public class Comment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long catId;
    private Long userId;
    private String content;
    private Integer isBlocked;
    private LocalDateTime createdAt;
}
