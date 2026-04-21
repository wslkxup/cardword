package com.cardword.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cardword.entity.Card;
import com.cardword.service.CardService;
import com.cardword.service.FileUploadService;
import com.cardword.util.SessionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    @Autowired
    private CardService cardService;

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private SessionUtil sessionUtil;

    private Long getCurrentUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("currentUserId");
        if (attr instanceof Long) return (Long) attr;
        return sessionUtil.getUserId(request);
    }

    @GetMapping
    public IPage<Card> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        Long viewerId = sessionUtil.getUserId(request);
        return cardService.listCards(page, size, viewerId);
    }

    @GetMapping("/random")
    public Map<String, Object> random(
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String excludeIds,
            HttpServletRequest request) {
        Long viewerId = sessionUtil.getUserId(request);
        List<Long> excludeIdList = Collections.emptyList();
        if (excludeIds != null && !excludeIds.trim().isEmpty()) {
            excludeIdList = java.util.Arrays.stream(excludeIds.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::valueOf)
                    .collect(java.util.stream.Collectors.toList());
        }
        return cardService.randomCards(size, excludeIdList, viewerId);
    }

    @PostMapping
    public Object publish(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);

        Object contentObj = body.get("content");
        Object nicknameObj = body.get("nickname");
        Object imageUrlObj = body.get("imageUrl");

        String content = contentObj != null ? contentObj.toString() : null;
        String nickname = nicknameObj != null ? nicknameObj.toString() : null;

        String imageUrl = null;
        if (imageUrlObj != null) {
            String raw = imageUrlObj.toString().trim();
            if (!raw.isEmpty() && !"null".equalsIgnoreCase(raw)) {
                imageUrl = raw;
            }
        }

        Integer isAnonymous = body.get("isAnonymous") != null
                ? Integer.valueOf(body.get("isAnonymous").toString())
                : 0;

        List<String> tagNames = Collections.emptyList();
        Object tagsObj = body.get("tags");
        if (tagsObj instanceof List) {
            List<?> raw = (List<?>) tagsObj;
            tagNames = raw.stream()
                    .filter(v -> v != null)
                    .map(Object::toString)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> s.length() > 12 ? s.substring(0, 12) : s)
                    .distinct()
                    .limit(5)
                    .collect(Collectors.toList());
        }

        return cardService.publish(content, nickname, userId, imageUrl, isAnonymous, tagNames);
    }

    @PostMapping("/{id}/like")
    public Map<String, Object> like(@PathVariable Long id) {
        cardService.likeCard(id);
        Card card = cardService.getById(id);
        return Collections.singletonMap("likesCount", card.getLikesCount());
    }

    @GetMapping("/my")
    public Object myCards(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        return cardService.listByUserId(userId, page, size, userId);
    }

    @GetMapping("/by-tag")
    public IPage<Card> byTag(
            @RequestParam("tagId") Long tagId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        Long viewerId = sessionUtil.getUserId(request);
        return cardService.listByTagId(tagId, page, size, viewerId);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        boolean success = cardService.deleteCard(id, userId);
        if (success) {
            return Collections.singletonMap("success", true);
        } else {
            return Collections.singletonMap("error", "无权删除该卡片或卡片不存在");
        }
    }

    @PostMapping("/upload")
    public Map<String, Object> uploadImage(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        try {
            String url = fileUploadService.uploadImage(file);
            return Collections.singletonMap("url", url);
        } catch (Exception e) {
            return Collections.singletonMap("error", e.getMessage());
        }
    }

    @PostMapping("/{id}/follow")
    public Object follow(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        boolean followed = cardService.toggleFollow(userId, id);
        return Collections.singletonMap("followed", followed);
    }

    @GetMapping("/followed-ids")
    public Object followedCardIds(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        return cardService.listFollowedCardIds(userId);
    }

    @GetMapping("/followed")
    public Object followedCards(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        return cardService.listFollowedCards(userId, page, size, userId);
    }
}
