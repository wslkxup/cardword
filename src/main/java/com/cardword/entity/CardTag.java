package com.cardword.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("card_tag")
public class CardTag {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long cardId;
    private Long tagId;
    private LocalDateTime createdAt;
}
