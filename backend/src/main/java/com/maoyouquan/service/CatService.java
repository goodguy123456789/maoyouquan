package com.maoyouquan.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.maoyouquan.dto.CatSubmitRequest;
import com.maoyouquan.entity.Cat;
import com.maoyouquan.entity.CatLike;
import com.maoyouquan.mapper.CatLikeMapper;
import com.maoyouquan.mapper.CatMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CatService {
    private final CatMapper catMapper;
    private final CatLikeMapper catLikeMapper;

    public Page<Cat> listApproved(int page, int size, String name, String breed, String sort) {
        QueryWrapper<Cat> qw = new QueryWrapper<Cat>().eq("status", "APPROVED");
        if (StringUtils.hasText(name)) qw.like("name", name);
        if (StringUtils.hasText(breed)) qw.like("breed", breed);
        if ("likes".equals(sort)) qw.orderByDesc("like_count");
        else qw.orderByDesc("created_at");
        return catMapper.selectPage(new Page<>(page, size), qw);
    }

    public Cat getById(Long id) {
        Cat cat = catMapper.selectById(id);
        if (cat == null || !"APPROVED".equals(cat.getStatus())) throw new RuntimeException("猫咪不存在");
        return cat;
    }

    public void submitCat(CatSubmitRequest req, Long submitterId) {
        Cat cat = new Cat();
        cat.setName(req.getName());
        cat.setGender(req.getGender());
        cat.setAge(req.getAge());
        cat.setBreed(req.getBreed());
        cat.setAvatarUrl(req.getAvatarUrl());
        cat.setPersonality(req.getPersonality());
        cat.setLikeCount(0);
        cat.setStatus("PENDING");
        cat.setSubmitterId(submitterId);
        catMapper.insert(cat);
    }

    @Transactional
    public Map<String, Object> toggleLike(Long catId, Long userId) {
        QueryWrapper<CatLike> qw = new QueryWrapper<CatLike>()
                .eq("cat_id", catId).eq("user_id", userId);
        CatLike existing = catLikeMapper.selectOne(qw);
        boolean liked;
        if (existing == null) {
            CatLike like = new CatLike();
            like.setCatId(catId);
            like.setUserId(userId);
            catLikeMapper.insert(like);
            catMapper.update(null, new UpdateWrapper<Cat>()
                    .eq("id", catId).setSql("like_count = like_count + 1"));
            liked = true;
        } else {
            catLikeMapper.delete(qw);
            catMapper.update(null, new UpdateWrapper<Cat>()
                    .eq("id", catId).setSql("like_count = like_count - 1"));
            liked = false;
        }
        Cat cat = catMapper.selectById(catId);
        return Map.of("liked", liked, "likeCount", cat.getLikeCount());
    }

    public boolean hasLiked(Long catId, Long userId) {
        return catLikeMapper.selectOne(new QueryWrapper<CatLike>()
                .eq("cat_id", catId).eq("user_id", userId)) != null;
    }
}
