package com.cardword.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cardword.entity.Comment;
import com.cardword.entity.User;
import com.cardword.mapper.CommentMapper;
import com.cardword.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 评论服务层
 * 处理评论的查询、发表等业务逻辑
 * 继承 MyBatis-Plus 的 ServiceImpl 获得通用 CRUD 能力
 */
@Service
public class CommentService extends ServiceImpl<CommentMapper, Comment> {

    @Autowired
    private UserMapper userMapper;

    /**
     * 查询指定卡片下的所有评论
     * 按创建时间正序排列，并批量填充每条评论的发布者昵称
     *
     * @param cardId 卡片ID
     * @return 评论列表（每条评论包含 nickname 字段）
     */
    public List<Comment> listByCardId(Long cardId) {
        List<Comment> comments = lambdaQuery()
                .eq(Comment::getCardId, cardId)
                .orderByAsc(Comment::getCreatedAt)
                .list();
        // 批量填充评论者昵称，避免在模板层逐条查询
        fillNicknames(comments);
        return comments;
    }

    /**
     * 发表评论（必须是已登录用户）
     * 根据 userId 查找用户，确认用户存在后创建评论记录
     *
     * @param cardId  评论所属的卡片ID
     * @param content 评论内容
     * @param userId  发表者的用户ID
     * @return 创建好的评论对象（包含发布者昵称）
     * @throws RuntimeException 当 userId 对应的用户不存在时抛出异常
     */
    public Comment addComment(Long cardId, String content, Long userId, Long parentId) {
        // 查询用户是否存在，防止传入伪造的 userId
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 创建评论记录并入库
        Comment comment = new Comment();
        comment.setCardId(cardId);
        comment.setUserId(user.getId());
        comment.setContent(content);

        // 若指定了父评论，校验其存在且属于同一张卡片
        if (parentId != null) {
            Comment parent = getById(parentId);
            if (parent == null || !parent.getCardId().equals(cardId)) {
                throw new RuntimeException("被回复的评论不存在");
            }
            comment.setParentId(parentId);
            // 冗余存储被回复者昵称，避免前端额外查询
            User replyToUser = userMapper.selectById(parent.getUserId());
            comment.setReplyToNickname(replyToUser != null ? replyToUser.getNickname() : "匿名用户");
        }

        save(comment);

        // 设置昵称用于返回给前端显示
        comment.setNickname(user.getNickname());
        return comment;
    }

    /**
     * 批量填充评论列表中每条评论的发布者昵称
     * 先收集所有去重的 userId，一次性批量查询用户表，
     * 然后通过 Map 映射回每条评论，避免 N+1 查询问题
     *
     * @param comments 需要填充昵称的评论列表
     */
    private void fillNicknames(List<Comment> comments) {
        if (comments.isEmpty()) return;

        // 收集所有评论涉及的用户ID（去重）
        Set<Long> userIds = comments.stream()
                .map(Comment::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) return;

        // 批量查询用户，构建 userId -> nickname 的映射
        Map<Long, String> nickMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getNickname));

        // 为每条评论设置昵称，用户不存在时默认显示"匿名用户"
        comments.forEach(c -> c.setNickname(nickMap.getOrDefault(c.getUserId(), "匿名用户")));
    }
}
