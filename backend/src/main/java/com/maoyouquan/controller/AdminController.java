package com.maoyouquan.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.maoyouquan.dto.NewsRequest;
import com.maoyouquan.dto.R;
import com.maoyouquan.entity.Cat;
import com.maoyouquan.entity.Comment;
import com.maoyouquan.entity.User;
import com.maoyouquan.mapper.CatMapper;
import com.maoyouquan.mapper.CommentMapper;
import com.maoyouquan.mapper.UserMapper;
import com.maoyouquan.service.CommentService;
import com.maoyouquan.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final CatMapper catMapper;
    private final CommentMapper commentMapper;
    private final CommentService commentService;
    private final NewsService newsService;
    private final UserMapper userMapper;

    @GetMapping("/cats/pending")
    public R<?> pendingCats(@RequestParam(defaultValue = "1") int page,
                             @RequestParam(defaultValue = "10") int size,
                             @RequestParam(defaultValue = "PENDING") String status,
                             @RequestParam(required = false) String name,
                             @RequestParam(required = false) String breed,
                             @RequestParam(required = false) String gender) {
        QueryWrapper<Cat> wrapper = new QueryWrapper<Cat>().orderByAsc("created_at");
        if (status != null && !status.isBlank()) wrapper.eq("status", status);
        if (name != null && !name.isBlank()) wrapper.like("name", name);
        if (breed != null && !breed.isBlank()) wrapper.eq("breed", breed);
        if (gender != null && !gender.isBlank()) wrapper.eq("gender", gender);
        return R.ok(catMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @PutMapping("/cats/{id}/status")
    public R<?> updateCatStatus(@PathVariable Long id,
                                 @RequestBody Map<String, String> body) {
        Cat cat = new Cat();
        cat.setId(id);
        cat.setStatus(body.get("status"));
        catMapper.updateById(cat);
        return R.ok();
    }

    @PutMapping("/comments/{id}/block")
    public R<?> blockComment(@PathVariable Long id) {
        commentService.blockComment(id);
        return R.ok();
    }

    @GetMapping("/news")
    public R<?> listNews(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) String title,
                          @RequestParam(required = false) String category) {
        return R.ok(newsService.adminList(page, size, title, category));
    }

    @PostMapping("/news")
    public R<?> createNews(@RequestBody NewsRequest req,
                           @AuthenticationPrincipal UserDetails userDetails) {
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", userDetails.getUsername()));
        newsService.create(req, user.getId());
        return R.ok();
    }

    @PutMapping("/news/{id}")
    public R<?> updateNews(@PathVariable Long id, @RequestBody NewsRequest req) {
        newsService.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/news/{id}")
    public R<?> deleteNews(@PathVariable Long id) {
        newsService.delete(id);
        return R.ok();
    }

    @GetMapping("/comments")
    public R<?> allComments(@RequestParam(defaultValue = "1") int page,
                             @RequestParam(defaultValue = "20") int size,
                             @RequestParam(required = false) String content,
                             @RequestParam(required = false) Integer isBlocked) {
        QueryWrapper<Comment> wrapper = new QueryWrapper<Comment>().orderByDesc("created_at");
        if (content != null && !content.isBlank()) wrapper.like("content", content);
        if (isBlocked != null) wrapper.eq("is_blocked", isBlocked);
        Page<Comment> result = commentMapper.selectPage(new Page<>(page, size), wrapper);
        return R.ok(result);
    }

    @GetMapping("/users")
    public R<?> allUsers(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "20") int size,
                          @RequestParam(required = false) Long id,
                          @RequestParam(required = false) String username) {
        QueryWrapper<User> wrapper = new QueryWrapper<User>().orderByAsc("created_at");
        if (id != null) wrapper.eq("id", id);
        if (username != null && !username.isBlank()) wrapper.like("username", username);
        Page<User> result = userMapper.selectPage(new Page<>(page, size), wrapper);
        result.getRecords().forEach(u -> u.setPasswordHash(null));
        return R.ok(result);
    }

    @PutMapping("/users/{id}/role")
    public R<?> updateUserRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        User user = new User();
        user.setId(id);
        user.setRole(body.get("role"));
        userMapper.updateById(user);
        return R.ok();
    }

    @PutMapping("/users/{id}/status")
    public R<?> updateUserStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        User user = new User();
        user.setId(id);
        user.setIsActive(body.get("isActive"));
        userMapper.updateById(user);
        return R.ok();
    }
}
