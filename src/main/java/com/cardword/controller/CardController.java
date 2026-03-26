package com.cardword.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cardword.entity.Card;
import com.cardword.service.CardService;
import com.cardword.service.FileUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 卡片控制器
 * 提供卡片的增删查、点赞等 RESTful 接口
 * 所有接口路径以 /api/cards 为前缀
 */
@RestController
@RequestMapping("/api/cards")
public class CardController {

    @Autowired
    private CardService cardService;

    @Autowired
    private FileUploadService fileUploadService;

    /**
     * 分页获取全部卡片列表（按时间倒序）
     * 用于首页 "全部" 标签页的数据加载
     */
    @GetMapping
    public IPage<Card> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return cardService.listCards(page, size);
    }

    /**
     * 随机获取卡片列表
     * 用于首页 "换一批" 功能，每次返回随机排序的卡片
     */
    @GetMapping("/random")
    public List<Card> random(@RequestParam(defaultValue = "10") int size) {
        return cardService.randomCards(size);
    }

    /**
     * 发布新卡片
     * 从请求体中提取内容、昵称和用户ID，未登录时 userId 为 null，
     * 后端会自动使用匿名用户身份发布
     */
    @PostMapping
    public Card publish(@RequestBody Map<String, Object> body) {
        String content = (String) body.get("content");
        String nickname = (String) body.getOrDefault("nickname", "匿名用户");
        Long userId = body.get("userId") != null ? Long.valueOf(body.get("userId").toString()) : null;
        String imageUrl = (String) body.get("imageUrl");
        return cardService.publish(content, nickname, userId, imageUrl);
    }

    /**
     * 卡片点赞接口
     * 使用 SQL 原子操作 +1，返回更新后的点赞数
     */
    @PostMapping("/{id}/like")
    public Map<String, Object> like(@PathVariable Long id) {
        cardService.likeCard(id);
        Card card = cardService.getById(id);
        return Collections.singletonMap("likesCount", card.getLikesCount());
    }

    /**
     * 获取指定用户发布的卡片列表（"我的"标签页）
     * 根据 userId 过滤，支持分页
     */
    @GetMapping("/my")
    public IPage<Card> myCards(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return cardService.listByUserId(userId, page, size);
    }

    /**
     * 删除卡片接口（仅限卡片发布者本人操作）
     * 前端需要在请求参数中携带 userId 表明当前登录用户，
     * 后端会校验该 userId 是否是卡片的发布者，防止越权删除
     *
     * @param id     要删除的卡片 ID（路径参数）
     * @param userId 当前登录用户 ID（查询参数）
     * @return 删除结果：成功返回 {"success": true}，失败返回 {"error": "..."}
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id, @RequestParam Long userId) {
        boolean success = cardService.deleteCard(id, userId);
        if (success) {
            return Collections.singletonMap("success", true);
        } else {
            return Collections.singletonMap("error", "无权删除该卡片或卡片不存在");
        }
    }

    /**
     * 上传图片接口
     */
    @PostMapping("/upload")
    public Map<String, Object> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String url = fileUploadService.uploadImage(file);
            return Collections.singletonMap("url", url);
        } catch (Exception e) {
            return Collections.singletonMap("error", e.getMessage());
        }
    }

    /**
     * 追/取消追卡片（toggle）
     */
    @PostMapping("/{id}/follow")
    public Map<String, Object> follow(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long userId = body.get("userId") != null ? Long.valueOf(body.get("userId").toString()) : null;
        if (userId == null) {
            return Collections.singletonMap("error", "请先登录");
        }
        boolean followed = cardService.toggleFollow(userId, id);
        return Collections.singletonMap("followed", followed);
    }

    /**
     * 获取用户追的卡片列表
     */
    @GetMapping("/followed")
    public IPage<Card> followedCards(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return cardService.listFollowedCards(userId, page, size);
    }
}
