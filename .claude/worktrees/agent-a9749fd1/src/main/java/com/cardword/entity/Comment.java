package com.cardword.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("comment")
public class Comment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long cardId;
    private Long userId;
    private String content;
    private Long parentId;
    private String replyToNickname;
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String nickname;
}
