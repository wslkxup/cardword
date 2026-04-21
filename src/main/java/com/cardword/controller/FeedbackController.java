package com.cardword.controller;

import com.cardword.entity.Feedback;
import com.cardword.mapper.FeedbackMapper;
import com.cardword.util.SessionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    @Autowired
    private FeedbackMapper feedbackMapper;

    @Autowired
    private SessionUtil sessionUtil;

    private Long getCurrentUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("currentUserId");
        if (attr instanceof Long) return (Long) attr;
        return sessionUtil.getUserId(request);
    }

    @PostMapping
    public Map<String, Object> submit(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);

        String title = (String) body.get("title");
        String content = (String) body.get("content");

        if (title == null || title.trim().isEmpty()) {
            return Collections.singletonMap("error", "问题标题不能为空");
        }
        if (content == null || content.trim().isEmpty()) {
            return Collections.singletonMap("error", "问题描述不能为空");
        }
        if (title.trim().length() > 100) {
            return Collections.singletonMap("error", "标题不能超过100字");
        }
        if (content.trim().length() > 500) {
            return Collections.singletonMap("error", "描述不能超过500字");
        }

        Feedback feedback = new Feedback();
        feedback.setUserId(userId);
        feedback.setTitle(title.trim());
        feedback.setContent(content.trim());
        feedback.setStatus(0);
        feedbackMapper.insert(feedback);

        return Collections.singletonMap("success", true);
    }
}
