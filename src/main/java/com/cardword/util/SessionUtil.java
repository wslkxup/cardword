package com.cardword.util;

import com.cardword.entity.UserSession;
import com.cardword.mapper.UserSessionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class SessionUtil {

    @Autowired
    private UserSessionMapper userSessionMapper;

    private static final int SESSION_DAYS = 7;

    /**
     * 从请求头中取出 token，验证并返回对应的 userId。
     * token 过期或不存在时返回 null。
     */
    public Long getUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) return null;
        UserSession session = userSessionMapper.selectById(token);
        if (session == null) return null;
        if (session.getCreatedAt().plusDays(SESSION_DAYS).isBefore(LocalDateTime.now())) {
            userSessionMapper.deleteById(token);
            return null;
        }
        return session.getUserId();
    }

    /**
     * 为指定用户创建新 session，返回 token。
     */
    public String createSession(Long userId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        UserSession session = new UserSession();
        session.setToken(token);
        session.setUserId(userId);
        session.setCreatedAt(LocalDateTime.now());
        userSessionMapper.insert(session);
        return token;
    }

    /**
     * 删除当前请求对应的 session（退出登录）。
     */
    public void deleteSession(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && !token.isEmpty()) {
            userSessionMapper.deleteById(token);
        }
    }
}
