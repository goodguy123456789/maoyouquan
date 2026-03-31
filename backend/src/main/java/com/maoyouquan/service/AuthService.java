package com.maoyouquan.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.maoyouquan.dto.LoginRequest;
import com.maoyouquan.dto.RegisterRequest;
import com.maoyouquan.entity.User;
import com.maoyouquan.mapper.UserMapper;
import com.maoyouquan.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public void register(RegisterRequest req) {
        if (userMapper.selectOne(new QueryWrapper<User>().eq("username", req.getUsername())) != null) {
            throw new RuntimeException("用户名已存在");
        }
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setEmail(req.getEmail());
        user.setNickname(req.getNickname() != null ? req.getNickname() : req.getUsername());
        user.setRole("USER");
        user.setIsActive(1);
        userMapper.insert(user);
    }

    public Map<String, String> login(LoginRequest req) {
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", req.getUsername()));
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("用户名或密码错误");
        }
        String token = tokenProvider.generateToken(user.getUsername(), user.getRole());
        return Map.of("token", token, "role", user.getRole(), "nickname", user.getNickname() != null ? user.getNickname() : user.getUsername());
    }

    public User getCurrentUser(String username) {
        return userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
    }
}
