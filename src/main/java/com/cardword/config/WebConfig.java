package com.cardword.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${upload.path:./uploads/images}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置静态资源处理器，让上传的图片可以被访问
        File uploadDir = new File(uploadPath).getAbsoluteFile();
        String location = uploadDir.toURI().toString();
        
        System.out.println("========== 静态资源配置 ==========");
        System.out.println("配置的上传路径: " + uploadPath);
        System.out.println("绝对路径: " + uploadDir.getAbsolutePath());
        System.out.println("资源位置: " + location);
        System.out.println("================================");
        
        registry.addResourceHandler("/api/uploads/**")
                .addResourceLocations(location);
    }
}
