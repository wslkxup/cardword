package com.cardword.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cardword.entity.Tag;
import com.cardword.mapper.TagMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    @Autowired
    private TagMapper tagMapper;

    @GetMapping("/search")
    public List<Tag> search(
            @RequestParam("q") String q,
            @RequestParam(defaultValue = "10") int limit
    ) {
        if (q == null) return Collections.emptyList();
        String keyword = q.trim();
        if (keyword.isEmpty()) return Collections.emptyList();

        if (limit <= 0) {
            limit = 10;
        }
        if (limit > 20) {
            limit = 20;
        }

        return tagMapper.selectList(
                new QueryWrapper<Tag>()
                        .select("id", "name", "use_count")
                        .like("name", keyword)
                        .orderByDesc("use_count")
                        .orderByAsc("name")
                        .last("LIMIT " + limit)
        );
    }

    @GetMapping("/hot")
    public List<Tag> hot(@RequestParam(defaultValue = "20") int limit) {
        if (limit <= 0) {
            limit = 20;
        }
        if (limit > 50) {
            limit = 50;
        }

        return tagMapper.selectList(
                new QueryWrapper<Tag>()
                        .select("id", "name", "use_count")
                        .gt("use_count", 0)
                        .orderByDesc("use_count")
                        .orderByAsc("name")
                        .last("LIMIT " + limit)
        );
    }
}
