package com.maoyouquan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("cat_likes")
public class CatLike {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long catId;
    private Long userId;
    private LocalDateTime createdAt;
}
