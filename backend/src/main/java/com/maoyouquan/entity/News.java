package com.maoyouquan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("news")
public class News {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    private String category;  // KNOWLEDGE | ANNOUNCEMENT | MIGRATION
    private String coverUrl;
    private Long authorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
