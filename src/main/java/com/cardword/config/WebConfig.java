package com.cardword.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${upload.path:./uploads/images}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置静态资源处理器，让上传的图片可以被访问
        // Windows 路径需要特殊处理
        String uploadDir = new java.io.File(uploadPath).getAbsolutePath().replace("\\", "/");
        registry.addResourceHandler("/api/uploads/**")
                .addResourceLocations("file:/" + uploadDir + "/");
    }
}
