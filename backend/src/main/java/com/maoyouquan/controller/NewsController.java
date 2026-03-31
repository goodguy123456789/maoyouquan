package com.maoyouquan.controller;

import com.maoyouquan.dto.R;
import com.maoyouquan.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {
    private final NewsService newsService;

    @GetMapping
    public R<?> list(@RequestParam(defaultValue = "1") int page,
                     @RequestParam(defaultValue = "10") int size,
                     @RequestParam(required = false) String category) {
        return R.ok(newsService.list(page, size, category));
    }

    @GetMapping("/{id}")
    public R<?> detail(@PathVariable Long id) {
        return R.ok(newsService.getById(id));
    }
}
