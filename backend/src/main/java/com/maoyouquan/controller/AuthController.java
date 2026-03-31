package com.maoyouquan.controller;

import com.maoyouquan.dto.LoginRequest;
import com.maoyouquan.dto.R;
import com.maoyouquan.dto.RegisterRequest;
import com.maoyouquan.entity.User;
import com.maoyouquan.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public R<?> register(@Valid @RequestBody RegisterRequest req) {
        authService.register(req);
        return R.ok("注册成功");
    }

    @PostMapping("/login")
    public R<?> login(@Valid @RequestBody LoginRequest req) {
        return R.ok(authService.login(req));
    }

    @GetMapping("/me")
    public R<User> me(@AuthenticationPrincipal UserDetails userDetails) {
        User user = authService.getCurrentUser(userDetails.getUsername());
        user.setPasswordHash(null);
        return R.ok(user);
    }
}
