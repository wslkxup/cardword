package com.cardword.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cardword.entity.Announcement;
import com.cardword.mapper.AnnouncementMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    @Autowired
    private AnnouncementMapper announcementMapper;

    @GetMapping
    public List<Announcement> list() {
        return announcementMapper.selectList(
                new QueryWrapper<Announcement>().orderByDesc("created_at")
        );
    }

    @GetMapping("/latest")
    public Map<String, Object> latest(@RequestParam(required = false) String since) {
        if (since != null && !since.isEmpty()) {
            LocalDateTime sinceTime = LocalDateTime.parse(since, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            long count = announcementMapper.selectCount(
                    new QueryWrapper<Announcement>().gt("created_at", sinceTime)
            );
            return Collections.singletonMap("hasNew", count > 0);
        }
        Announcement latestOne = announcementMapper.selectOne(
                new QueryWrapper<Announcement>().orderByDesc("created_at").last("LIMIT 1")
        );
        if (latestOne != null) {
            Map<String, Object> result = new HashMap<>();
            result.put("hasNew", true);
            result.put("latestTime", latestOne.getCreatedAt().toString());
            return result;
        }
        return Collections.singletonMap("hasNew", false);
    }
}
