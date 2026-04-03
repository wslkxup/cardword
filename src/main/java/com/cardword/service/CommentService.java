package com.cardword.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cardword.entity.Card;
import com.cardword.entity.Comment;
import com.cardword.entity.User;
import com.cardword.mapper.CardMapper;
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
    
    @Autowired
    private CardMapper cardMapper;

    @Autowired
    private UserService userService;

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
        // 查询卡片信息，用于判断是否是匿名卡片和获取卡片作者信息
        Card card = cardMapper.selectById(cardId);
        if (card == null) {
            throw new RuntimeException("卡片不存在");
        }
        boolean isAnonymousCard = card.getIsAnonymous() != null && card.getIsAnonymous() == 1;
        boolean isCardAuthor = userId.equals(card.getUserId());

        // 创建评论记录并入库
        Comment comment = new Comment();
        comment.setCardId(cardId);
        comment.setUserId(userId);
        comment.setContent(content);

        // 设置评论者昵称（冗余存储到 username 字段）
        if (isCardAuthor) {
            // 如果是卡片作者，直接从卡片表获取 username
            if (isAnonymousCard) {
                comment.setUsername("匿名卡片作者");
            } else {
                comment.setUsername(card.getUsername());
            }
        } else {
            // 如果不是卡片作者，需要查询用户表获取昵称
            User user = userMapper.selectById(userId);
            if (user == null) {
                throw new RuntimeException("用户不存在");
            }
            comment.setUsername(user.getNickname());
        }

        // 若指定了父评论，校验其存在且属于同一张卡片
        if (parentId != null) {
            Comment parent = getById(parentId);
            if (parent == null || !parent.getCardId().equals(cardId)) {
                throw new RuntimeException("被回复的评论不存在");
            }
            comment.setParentId(parentId);
            // 记录被回复者的用户 ID
            comment.setReplyToUserId(parent.getUserId());
            // 直接设置回复对象的昵称（用于立即返回给前端）
            // 如果是匿名卡片且被回复者是卡片作者，则显示"匿名卡片作者"
            if (isAnonymousCard && parent.getUserId().equals(card.getUserId())) {
                comment.setReplyToNickname("匿名卡片作者");
            } else {
                // 从父评论的 username 字段获取
                comment.setReplyToNickname(parent.getUsername());
            }
        }

        save(comment);

        // 参与评论奖励 1 经验值
        userService.addExp(userId, 1);

        // 设置昵称用于返回给前端显示（直接使用 username 字段）
        comment.setNickname(comment.getUsername());
        return comment;
    }

    /**
     * 批量填充评论列表中每条评论的发布者昵称
     * 直接从评论表的 username 字段获取，不需要查询用户表
     *
     * @param comments 需要填充昵称的评论列表
     */
    private void fillNicknames(List<Comment> comments) {
        if (comments.isEmpty()) return;

        // 为每条评论设置昵称（直接使用 username 字段）
        comments.forEach(c -> {
            c.setNickname(c.getUsername());
            
            // 设置回复对象的昵称
            if (c.getParentId() != null) {
                // 从评论列表中查找父评论（使用 parentId 查找）
                Comment replyToComment = comments.stream()
                        .filter(rc -> rc.getId().equals(c.getParentId()))
                        .findFirst()
                        .orElse(null);
                if (replyToComment != null) {
                    c.setReplyToNickname(replyToComment.getUsername());
                }
            }
        });
    }
}
