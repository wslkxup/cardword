package com.cardword.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论实体类
 * 对应数据库中的 comment 表
 */
@Data
@TableName("comment")
public class Comment {
    /** 评论ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 卡片ID，关联的卡片 */
    private Long cardId;
    
    /** 评论用户ID */
    private Long userId;
    
    /** 评论者昵称（冗余存储，避免查询用户表） */
    private String username;
    
    /** 评论内容 */
    private String content;
    
    /** 父评论ID，用于实现嵌套评论，0表示顶层评论 */
    private Long parentId;
    
    /** 评论创建时间 */
    private LocalDateTime createdAt;

    /** 评论者昵称，非数据库字段 */
    @TableField(exist = false)
    private String nickname;

    /** 被回复的用户ID，非数据库字段 */
    @TableField(exist = false)
    private Long replyToUserId;

    /** 被回复用户的昵称，非数据库字段 */
    @TableField(exist = false)
    private String replyToNickname;
}
