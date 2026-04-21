package com.cardword.controller;

import com.cardword.service.FileUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    @Autowired
    private FileUploadService fileUploadService;

    @GetMapping("/config")
    public Map<String, Object> config() {
        Map<String, Object> config = new HashMap<>();
        config.put("maxSizeBytes", fileUploadService.getMaxSizeBytes());
        config.put("maxSizeDisplay", fileUploadService.getMaxSizeDisplay());
        Set<String> types = fileUploadService.getAllowedMimeTypes();
        config.put("allowedTypes", types);
        config.put("accept", mimeTypeToAccept(types));
        return config;
    }

    private String mimeTypeToAccept(Set<String> mimeTypes) {
        StringBuilder sb = new StringBuilder();
        for (String mime : mimeTypes) {
            if (sb.length() > 0) sb.append(",");
            switch (mime) {
                case "image/jpeg":
                    sb.append(".jpg,.jpeg");
                    break;
                case "image/png":
                    sb.append(".png");
                    break;
                case "image/webp":
                    sb.append(".webp");
                    break;
                case "image/gif":
                    sb.append(".gif");
                    break;
                default:
                    sb.append(mime);
            }
        }
        return sb.toString();
    }
}
