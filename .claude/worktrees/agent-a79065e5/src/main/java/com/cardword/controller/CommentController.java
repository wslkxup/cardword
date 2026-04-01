package com.cardword.controller;

import com.cardword.entity.Comment;
import com.cardword.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 评论控制器
 * 提供评论的查询和发表接口
 * 接口路径以 /api/cards/{cardId}/comments 为前缀，嵌套在卡片资源下
 */
@RestController
@RequestMapping("/api/cards/{cardId}/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;

    /**
     * 获取指定卡片下的所有评论
     * 按创建时间正序排列（最早的在前）
     *
     * @param cardId 卡片ID（路径参数）
     * @return 评论列表，每条评论包含发布者昵称
     */
    @GetMapping
    public List<Comment> list(@PathVariable Long cardId) {
        return commentService.listByCardId(cardId);
    }

    /**
     * 发表评论（需要登录）
     * 在 Controller 层进行登录校验：如果请求中没有 userId 则直接拒绝
     * 这样可以在进入 Service 之前就拦截未登录请求，避免不必要的数据库操作
     *
     * @param cardId 评论所属的卡片ID（路径参数）
     * @param body   请求体，需包含 content（评论内容）和 userId（用户ID）
     * @return 成功返回 Comment 对象，未登录返回 {"error": "请先登录后再评论"}
     */
    @PostMapping
    public Object add(@PathVariable Long cardId, @RequestBody Map<String, Object> body) {
        // 登录校验：userId 为空说明用户未登录
        Long userId = body.get("userId") != null ? Long.valueOf(body.get("userId").toString()) : null;
        if (userId == null) {
            return Collections.singletonMap("error", "请先登录后再评论");
        }
        String content = (String) body.get("content");
        Long parentId = body.get("parentId") != null ? Long.valueOf(body.get("parentId").toString()) : null;
        return commentService.addComment(cardId, content, userId, parentId);
    }
}
