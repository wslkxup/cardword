package com.cardword.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("card_follow")
public class CardFollow {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long cardId;
    private LocalDateTime createdAt;
}
