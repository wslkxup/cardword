package com.cardword.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@Service
public class FileUploadService {

    @Value("${upload.path}")
    private String uploadPath;

    @Value("${upload.max-size}")
    private String maxSize;

    @Value("${upload.allowed-types}")
    private String allowedTypes;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final int MAX_WIDTH = 1920;
    private static final int MAX_HEIGHT = 1920;
    private static final float COMPRESS_QUALITY = 0.8f;

    /**
     * 上传图片文件（带压缩）
     */
    public String uploadImage(MultipartFile file) throws IOException {
        // 1. 验证文件
        validateFile(file);

        // 2. 创建上传目录
        Path uploadDir = Paths.get(uploadPath).toAbsolutePath().normalize();
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        // 3. 生成文件名（日期 + UUID）
        String dateDir = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
        Path datePath = uploadDir.resolve(dateDir);
        if (!Files.exists(datePath)) {
            Files.createDirectories(datePath);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
            ? originalFilename.substring(originalFilename.lastIndexOf("."))
            : ".jpg";
        String filename = UUID.randomUUID().toString().replace("-", "") + extension;

        // 4. 压缩并保存图片
        Path filePath = datePath.resolve(filename);
        compressAndSaveImage(file, filePath);

        // 5. 返回访问 URL
        return "/api/uploads/" + dateDir + "/" + filename;
    }

    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file) throws IOException {
        // 检查文件是否为空
        if (file.isEmpty()) {
            throw new IOException("上传文件不能为空");
        }

        // 检查文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IOException("文件大小不能超过 5MB");
        }

        // 检查文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IOException("只能上传图片文件");
        }
    }

    private void compressAndSaveImage(MultipartFile file, Path filePath) throws IOException {
        // 先将上传文件写入目标路径，确保 MultipartFile 的临时文件流被完全关闭
        try (java.io.InputStream in = file.getInputStream();) {
            Files.copy(in, filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        // 再从目标文件读取进行压缩，避免直接持有 MultipartFile 的底层临时文件句柄
        BufferedImage originalImage;
        try (java.io.InputStream in = Files.newInputStream(filePath)) {
            originalImage = ImageIO.read(in);
        }
        if (originalImage == null) {
            throw new IOException("无效的图片文件");
        }

        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        if (width > MAX_WIDTH || height > MAX_HEIGHT) {
            float ratio = Math.min((float) MAX_WIDTH / width, (float) MAX_HEIGHT / height);
            width = (int) (width * ratio);
            height = (int) (height * ratio);
        }

        BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(originalImage, 0, 0, width, height, null);
        g2d.dispose();

        // 覆盖写回 JPEG 文件
        File outputFile = filePath.toFile();
        ImageIO.write(resizedImage, "jpg", outputFile);
    }
}
