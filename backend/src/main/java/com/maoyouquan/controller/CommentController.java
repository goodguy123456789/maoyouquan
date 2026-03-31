package com.maoyouquan.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.maoyouquan.dto.CommentRequest;
import com.maoyouquan.dto.R;
import com.maoyouquan.entity.User;
import com.maoyouquan.mapper.UserMapper;
import com.maoyouquan.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cats/{catId}/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;
    private final UserMapper userMapper;

    @GetMapping
    public R<?> list(@PathVariable Long catId) {
        return R.ok(commentService.listByCat(catId));
    }

    @PostMapping
    public R<?> add(@PathVariable Long catId,
                    @RequestBody CommentRequest req,
                    @AuthenticationPrincipal UserDetails userDetails) {
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", userDetails.getUsername()));
        commentService.addComment(catId, user.getId(), req.getContent());
        return R.ok("评论成功");
    }
}
