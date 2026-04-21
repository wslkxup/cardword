package com.cardword.config;

import com.cardword.util.SessionUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private SessionUtil sessionUtil;

    private static final Set<String> PUBLIC_PATHS = new HashSet<>(Arrays.asList(
            "/api/users/login",
            "/api/users/register",
            "/api/cards",
            "/api/cards/random",
            "/api/cards/by-tag",
            "/api/announcements",
            "/api/announcements/latest",
            "/api/tags/search",
            "/api/tags/hot",
            "/api/upload/config"
    ));

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String uri = request.getRequestURI();
        if (isPublicPath(uri, request.getMethod())) {
            return true;
        }

        Long userId = sessionUtil.getUserId(request);
        if (userId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(response.getOutputStream(), Collections.singletonMap("error", "登录已过期，请重新登录"));
            return false;
        }

        request.setAttribute("currentUserId", userId);
        return true;
    }

    private boolean isPublicPath(String uri, String method) {
        if (PUBLIC_PATHS.contains(uri)) {
            return true;
        }

        if (uri.matches("/api/cards/\\d+/like") && "POST".equalsIgnoreCase(method)) {
            return true;
        }
        if (uri.matches("/api/cards/\\d+/comments") && "GET".equalsIgnoreCase(method)) {
            return true;
        }

        return false;
    }
}
