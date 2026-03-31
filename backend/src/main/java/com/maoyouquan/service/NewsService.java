package com.maoyouquan.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.maoyouquan.dto.NewsRequest;
import com.maoyouquan.entity.News;
import com.maoyouquan.mapper.NewsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class NewsService {
    private final NewsMapper newsMapper;

    public Page<News> list(int page, int size, String category) {
        QueryWrapper<News> qw = new QueryWrapper<News>().orderByDesc("created_at");
        if (StringUtils.hasText(category)) qw.eq("category", category);
        return newsMapper.selectPage(new Page<>(page, size), qw);
    }

    public News getById(Long id) {
        News news = newsMapper.selectById(id);
        if (news == null) throw new RuntimeException("新闻不存在");
        return news;
    }

    public void create(NewsRequest req, Long authorId) {
        News news = new News();
        news.setTitle(req.getTitle());
        news.setContent(req.getContent());
        news.setCategory(req.getCategory());
        news.setCoverUrl(req.getCoverUrl());
        news.setAuthorId(authorId);
        newsMapper.insert(news);
    }

    public void update(Long id, NewsRequest req) {
        News news = new News();
        news.setId(id);
        news.setTitle(req.getTitle());
        news.setContent(req.getContent());
        news.setCategory(req.getCategory());
        news.setCoverUrl(req.getCoverUrl());
        newsMapper.updateById(news);
    }

    public void delete(Long id) {
        newsMapper.deleteById(id);
    }
}
