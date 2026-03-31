package com.maoyouquan.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.maoyouquan.dto.CatSubmitRequest;
import com.maoyouquan.dto.R;
import com.maoyouquan.entity.Cat;
import com.maoyouquan.entity.User;
import com.maoyouquan.mapper.UserMapper;
import com.maoyouquan.service.CatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cats")
@RequiredArgsConstructor
public class CatController {
    private final CatService catService;
    private final UserMapper userMapper;

    @GetMapping
    public R<?> list(@RequestParam(defaultValue = "1") int page,
                     @RequestParam(defaultValue = "12") int size,
                     @RequestParam(required = false) String name,
                     @RequestParam(required = false) String breed,
                     @RequestParam(defaultValue = "latest") String sort) {
        return R.ok(catService.listApproved(page, size, name, breed, sort));
    }

    @GetMapping("/{id}")
    public R<Cat> detail(@PathVariable Long id) {
        return R.ok(catService.getById(id));
    }

    @PostMapping
    public R<?> submit(@Valid @RequestBody CatSubmitRequest req,
                       @AuthenticationPrincipal UserDetails userDetails) {
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", userDetails.getUsername()));
        catService.submitCat(req, user.getId());
        return R.ok("提交成功，等待审核");
    }

    @PostMapping("/{id}/like")
    public R<?> like(@PathVariable Long id,
                     @AuthenticationPrincipal UserDetails userDetails) {
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", userDetails.getUsername()));
        return R.ok(catService.toggleLike(id, user.getId()));
    }
}
