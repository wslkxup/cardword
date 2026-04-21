package com.cardword.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("card")
public class Card {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String username;
    private String content;
    private String imageUrl;
    private Integer isAnonymous;
    private Integer likesCount;
    private LocalDateTime createdAt;

    /**
     * 关联的用户对象（非数据库字段）
     * 用于在返回卡片数据时携带发布者的昵称、头像等信息
     * exist = false 表示该字段不对应数据库中的列，MyBatis-Plus 查询时会忽略它
     */
    @TableField(exist = false)
    private User user;

    /**
     * 该卡片的评论总数（非数据库字段）
     * 在查询卡片列表时由 Service 层统计并填充，前端用于在评论按钮上显示数量
     */
    @TableField(exist = false)
    private Integer commentCount;

    /**
     * 当前查看者是否为卡片作者（非数据库字段）
     * 用于前端安全判断当前用户是否可以删除等操作
     */
    @TableField(exist = false)
    private Boolean isOwner;

    /**
     * 卡片标签（非数据库字段）
     */
    @TableField(exist = false)
    private List<Tag> tags;
}
