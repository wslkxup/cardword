package com.cardword.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FileUploadService {

    @Value("${upload.path}")
    private String uploadPath;

    @Value("${upload.max-size}")
    private String maxSize;

    @Value("${upload.allowed-types}")
    private String allowedTypes;

    private static final int MAX_WIDTH = 1920;
    private static final int MAX_HEIGHT = 1920;
    private static final Set<String> DYNAMIC_FORMATS = new HashSet<>(Arrays.asList("gif"));

    /**
     * 上传图片文件（带压缩）
     */
    public String uploadImage(MultipartFile file) throws IOException {
        // 1. 验证文件
        String outputExtension = validateFile(file);

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

        String filename = UUID.randomUUID().toString().replace("-", "") + outputExtension;

        // 4. 压缩并保存图片
        Path filePath = datePath.resolve(filename);
        compressAndSaveImage(file, filePath);

        // 5. 返回访问 URL
        return "/api/uploads/" + dateDir + "/" + filename;
    }

    /**
     * 验证文件
     */
    private String validateFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("上传文件不能为空");
        }

        long maxFileSize = DataSize.parse(maxSize).toBytes();
        if (file.getSize() > maxFileSize) {
            throw new IOException("文件大小不能超过 " + maxSize);
        }

        Set<String> allowedMimeTypes = parseAllowedMimeTypes();
        String contentType = normalizeMimeType(file.getContentType());
        if (!allowedMimeTypes.contains(contentType)) {
            throw new IOException("只允许上传 JPG、PNG、WEBP 格式的静态图片");
        }

        ImageInfo imageInfo = readImageInfo(file);
        if (imageInfo == null || imageInfo.bufferedImage == null) {
            throw new IOException("无效的图片文件");
        }
        if (!allowedMimeTypes.contains(imageInfo.mimeType)) {
            throw new IOException("只允许上传 JPG、PNG、WEBP 格式的静态图片");
        }
        if (DYNAMIC_FORMATS.contains(imageInfo.formatName) || imageInfo.frameCount > 1) {
            throw new IOException("不支持上传动态图片");
        }

        return extensionForFormat(imageInfo.formatName);
    }

    private ImageInfo readImageInfo(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream();
             ImageInputStream imageInputStream = ImageIO.createImageInputStream(inputStream)) {
            if (imageInputStream == null) {
                return null;
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);
            if (!readers.hasNext()) {
                return null;
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInputStream, false, false);
                String formatName = reader.getFormatName().toLowerCase(Locale.ROOT);
                int frameCount = safeFrameCount(reader);
                BufferedImage bufferedImage = reader.read(0);
                return new ImageInfo(formatName, mimeTypeForFormat(formatName), frameCount, bufferedImage);
            } finally {
                reader.dispose();
            }
        }
    }

    private int safeFrameCount(ImageReader reader) {
        try {
            return reader.getNumImages(true);
        } catch (IOException ignored) {
            return 1;
        }
    }

    private Set<String> parseAllowedMimeTypes() {
        return Arrays.stream(allowedTypes.split(","))
            .map(this::normalizeMimeType)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toCollection(HashSet::new));
    }

    private String normalizeMimeType(String contentType) {
        return contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
    }

    private String mimeTypeForFormat(String formatName) throws IOException {
        switch (formatName) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "webp":
                return "image/webp";
            case "gif":
                return "image/gif";
            default:
                throw new IOException("不支持的图片格式");
        }
    }

    private String extensionForFormat(String formatName) throws IOException {
        switch (formatName) {
            case "jpg":
            case "jpeg":
                return ".jpg";
            case "png":
                return ".png";
            case "webp":
                return ".webp";
            default:
                throw new IOException("不支持的图片格式");
        }
    }

    private void compressAndSaveImage(MultipartFile file, Path filePath) throws IOException {
        ImageInfo imageInfo = readImageInfo(file);
        if (imageInfo == null || imageInfo.bufferedImage == null) {
            throw new IOException("无效的图片文件");
        }

        BufferedImage originalImage = imageInfo.bufferedImage;
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        if (width > MAX_WIDTH || height > MAX_HEIGHT) {
            float ratio = Math.min((float) MAX_WIDTH / width, (float) MAX_HEIGHT / height);
            width = (int) (width * ratio);
            height = (int) (height * ratio);
        }

        BufferedImage outputImage = needsRgbBackground(imageInfo.formatName)
            ? new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
            : new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = outputImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (needsRgbBackground(imageInfo.formatName)) {
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, width, height);
        } else {
            g2d.setComposite(AlphaComposite.Clear);
            g2d.fillRect(0, 0, width, height);
            g2d.setComposite(AlphaComposite.SrcOver);
        }
        g2d.drawImage(originalImage, 0, 0, width, height, null);
        g2d.dispose();

        File outputFile = filePath.toFile();
        if (!ImageIO.write(outputImage, imageInfo.formatName, outputFile)) {
            throw new IOException("图片保存失败");
        }
    }

    private boolean needsRgbBackground(String formatName) {
        return "jpg".equals(formatName) || "jpeg".equals(formatName);
    }

    public long getMaxSizeBytes() {
        return DataSize.parse(maxSize).toBytes();
    }

    public String getMaxSizeDisplay() {
        return maxSize;
    }

    public Set<String> getAllowedMimeTypes() {
        return parseAllowedMimeTypes();
    }

    private static class ImageInfo {
        private final String formatName;
        private final String mimeType;
        private final int frameCount;
        private final BufferedImage bufferedImage;

        private ImageInfo(String formatName, String mimeType, int frameCount, BufferedImage bufferedImage) {
            this.formatName = formatName;
            this.mimeType = mimeType;
            this.frameCount = frameCount;
            this.bufferedImage = bufferedImage;
        }
    }
}
