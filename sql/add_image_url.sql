-- 为 card 表添加 image_url 字段
ALTER TABLE card ADD COLUMN `image_url` varchar(500) DEFAULT NULL COMMENT '图片 URL' AFTER content;
