package com.maoyouquan.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.maoyouquan.entity.Comment;
import com.maoyouquan.mapper.CommentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentMapper commentMapper;

    public List<Comment> listByCat(Long catId) {
        return commentMapper.selectList(new QueryWrapper<Comment>()
                .eq("cat_id", catId).eq("is_blocked", 0).orderByAsc("created_at"));
    }

    public void addComment(Long catId, Long userId, String content) {
        Comment comment = new Comment();
        comment.setCatId(catId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setIsBlocked(0);
        commentMapper.insert(comment);
    }

    public void blockComment(Long commentId) {
        Comment c = new Comment();
        c.setId(commentId);
        c.setIsBlocked(1);
        commentMapper.updateById(c);
    }
}
