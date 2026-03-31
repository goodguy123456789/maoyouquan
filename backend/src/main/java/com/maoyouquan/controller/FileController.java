package com.maoyouquan.controller;

import com.maoyouquan.dto.R;
import com.maoyouquan.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;

    @PostMapping("/image")
    public R<?> upload(@RequestParam("file") MultipartFile file) throws IOException {
        String url = fileService.save(file);
        return R.ok(Map.of("url", url));
    }
}
