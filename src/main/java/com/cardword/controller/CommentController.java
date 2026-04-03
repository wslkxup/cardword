package com.cardword.controller;

import com.cardword.entity.Comment;
import com.cardword.service.CommentService;
import com.cardword.util.SessionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cards/{cardId}/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private SessionUtil sessionUtil;

    @GetMapping
    public List<Comment> list(@PathVariable Long cardId) {
        return commentService.listByCardId(cardId);
    }

    @PostMapping
    public Object add(@PathVariable Long cardId, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = sessionUtil.getUserId(request);
        if (userId == null) {
            return Collections.singletonMap("error", "请先登录后再评论");
        }
        String content = (String) body.get("content");
        Long parentId = body.get("parentId") != null ? Long.valueOf(body.get("parentId").toString()) : null;
        return commentService.addComment(cardId, content, userId, parentId);
    }
}
