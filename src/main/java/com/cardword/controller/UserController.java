package com.cardword.controller;

import com.cardword.entity.User;
import com.cardword.mapper.UserMapper;
import com.cardword.service.UserService;
import com.cardword.util.PasswordUtil;
import com.cardword.util.SessionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private SessionUtil sessionUtil;

    private Long getCurrentUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("currentUserId");
        if (attr instanceof Long) return (Long) attr;
        return sessionUtil.getUserId(request);
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, Object> body) {
        String nickname = body.get("nickname") != null ? ((String) body.get("nickname")).trim() : "";
        String pwd = body.get("pwd") != null ? (String) body.get("pwd") : "";

        if (nickname.isEmpty()) {
            return Collections.singletonMap("error", "用户名不能为空");
        }
        if (pwd.isEmpty()) {
            return Collections.singletonMap("error", "密码不能为空");
        }

        User existing = userMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                .eq("nickname", nickname));
        if (existing != null) {
            return Collections.singletonMap("error", "用户名已存在");
        }

        User user = new User();
        user.setNickname(nickname);
        user.setPwd(PasswordUtil.encrypt(pwd));
        userMapper.insert(user);

        String token = sessionUtil.createSession(user.getId());
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        return result;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, Object> body) {
        String nickname = (String) body.get("nickname");
        String pwd = (String) body.get("pwd");

        User user = userMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                .eq("nickname", nickname));

        if (user == null || !PasswordUtil.verify(pwd, user.getPwd())) {
            return Collections.singletonMap("error", "用户名或密码错误");
        }

        String token = sessionUtil.createSession(user.getId());
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        return result;
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpServletRequest request) {
        sessionUtil.deleteSession(request);
        return Collections.singletonMap("success", true);
    }

    @PostMapping("/changePassword")
    public Map<String, Object> changePassword(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);

        String oldPwd = body.get("oldPwd") != null ? (String) body.get("oldPwd") : "";
        String newPwd = body.get("newPwd") != null ? (String) body.get("newPwd") : "";

        if (oldPwd.isEmpty() || newPwd.isEmpty()) {
            return Collections.singletonMap("error", "密码不能为空");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            return Collections.singletonMap("error", "用户不存在");
        }

        if (!PasswordUtil.verify(oldPwd, user.getPwd())) {
            return Collections.singletonMap("error", "旧密码不正确");
        }

        user.setPwd(PasswordUtil.encrypt(newPwd));
        userMapper.updateById(user);

        return Collections.singletonMap("success", true);
    }

    @GetMapping("/info")
    public Map<String, Object> getUserInfo(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);

        User user = userService.getUserInfo(userId);
        if (user == null) {
            return Collections.singletonMap("error", "用户不存在");
        }

        int level = user.getLevel() != null ? user.getLevel() : 0;
        int exp = user.getExp() != null ? user.getExp() : 0;

        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("nickname", user.getNickname());
        result.put("exp", exp);
        result.put("level", level);
        result.put("currentLevelExp", userService.getCurrentLevelExp(exp, level));
        result.put("nextLevelExp", userService.getNextLevelExp(level));
        result.put("expNeeded", userService.getExpNeeded(exp, level));
        return result;
    }
}
