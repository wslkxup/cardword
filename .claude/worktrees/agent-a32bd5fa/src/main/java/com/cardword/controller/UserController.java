package com.cardword.controller;

import com.cardword.entity.User;
import com.cardword.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

/**
 * 用户控制器
 * 提供用户注册和登录接口
 * 所有接口路径以 /api/users 为前缀
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserMapper userMapper;

    /**
     * 用户注册接口
     * 校验规则：
     *   1. 用户名不能为空（会自动 trim 去除首尾空格）
     *   2. 密码不能为空
     *   3. 用户名不能与已有用户重复
     *
     * @param body 请求体，包含 nickname 和 pwd 字段
     * @return 成功返回 {"userId": id}，失败返回 {"error": "错误信息"}
     */
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, Object> body) {
        // 提取并清理用户名（去除首尾空格）
        String nickname = body.get("nickname") != null ? ((String) body.get("nickname")).trim() : "";
        String pwd = body.get("pwd") != null ? (String) body.get("pwd") : "";

        // 校验：用户名不能为空
        if (nickname.isEmpty()) {
            return Collections.singletonMap("error", "用户名不能为空");
        }
        // 校验：密码不能为空
        if (pwd.isEmpty()) {
            return Collections.singletonMap("error", "密码不能为空");
        }

        // 校验：检查用户名是否已被注册
        User existing = userMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                .eq("nickname", nickname));
        if (existing != null) {
            return Collections.singletonMap("error", "用户名已存在");
        }

        // 创建用户并入库
        User user = new User();
        user.setNickname(nickname);
        user.setPwd(pwd);
        userMapper.insert(user);

        return Collections.singletonMap("userId", user.getId());
    }

    /**
     * 用户登录接口
     * 根据用户名和密码查询匹配的用户记录
     *
     * @param body 请求体，包含 nickname 和 pwd 字段
     * @return 成功返回 {"userId": id}，失败返回 {"error": "用户名或密码错误"}
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, Object> body) {
        String nickname = (String) body.get("nickname");
        String pwd = (String) body.get("pwd");

        User user = userMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                .eq("nickname", nickname)
                .eq("pwd", pwd)
        );

        if (user != null) {
            return Collections.singletonMap("userId", user.getId());
        } else {
            return Collections.singletonMap("error", "用户名或密码错误");
        }
    }
}