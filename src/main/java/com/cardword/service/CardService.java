package com.cardword.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cardword.entity.Card;
import com.cardword.entity.CardFollow;
import com.cardword.entity.Comment;
import com.cardword.entity.User;
import com.cardword.mapper.CardFollowMapper;
import com.cardword.mapper.CardMapper;
import com.cardword.mapper.CommentMapper;
import com.cardword.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CardService extends ServiceImpl<CardMapper, Card> {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private CardFollowMapper cardFollowMapper;

    /**
     * 分页查询所有卡片列表
     * 按创建时间倒序排列，同时填充每张卡片的用户信息和评论数量
     *
     * @param page 当前页码（从1开始）
     * @param size 每页条数
     * @return 分页结果，包含卡片列表及分页元数据
     */
    public IPage<Card> listCards(int page, int size) {
        Page<Card> p = new Page<>(page, size);
        IPage<Card> cards = lambdaQuery()
                .orderByDesc(Card::getCreatedAt)
                .page(p);
        // 填充关联数据：用户信息 + 评论数量
        fillCardExtraInfo(cards.getRecords());
        return cards;
    }

    /**
     * 批量填充卡片的用户信息
     * 遍历卡片列表，根据每张卡片的 userId 查询对应的 User 对象并设置到 card.user 字段
     * 这样前端可以直接通过 card.user.nickname 获取发布者昵称
     *
     * @param cards 需要填充用户信息的卡片列表
     */
    private void fillUserInfo(List<Card> cards) {
        for (Card card : cards) {
            User user = userMapper.selectById(card.getUserId());
            if (user != null) {
                card.setUser(user);
            }
        }
    }

    /**
     * 批量填充卡片的评论数量
     * 收集所有卡片ID，一次性查询每张卡片对应的评论条数，
     * 避免在循环中逐个查询（N+1 问题），提升查询性能
     *
     * @param cards 需要填充评论数量的卡片列表
     */
    private void fillCommentCount(List<Card> cards) {
        if (cards.isEmpty()) return;

        // 收集所有卡片的 ID 集合
        Set<Long> cardIds = cards.stream()
                .map(Card::getId)
                .collect(Collectors.toSet());

        // 使用 MyBatis-Plus 的 QueryWrapper 按 card_id 分组统计评论数
        // SQL 等价于: SELECT card_id, COUNT(*) AS cnt FROM comment WHERE card_id IN (...) GROUP BY card_id
        List<Map<String, Object>> countList = commentMapper.selectMaps(
                new QueryWrapper<Comment>()
                        .select("card_id", "COUNT(*) AS cnt")
                        .in("card_id", cardIds)
                        .groupBy("card_id")
        );

        // 将查询结果转换为 Map<cardId, count> 方便快速查找
        Map<Long, Integer> countMap = countList.stream().collect(Collectors.toMap(
                m -> ((Number) m.get("card_id")).longValue(),
                m -> ((Number) m.get("cnt")).intValue()
        ));

        // 遍历卡片列表，设置评论数量，没有评论的卡片默认为 0
        for (Card card : cards) {
            card.setCommentCount(countMap.getOrDefault(card.getId(), 0));
        }
    }

    /**
     * 统一填充卡片的所有额外信息（用户信息 + 评论数量）
     * 所有查询卡片列表的方法都应调用此方法，确保返回数据的一致性
     *
     * @param cards 需要填充额外信息的卡片列表
     */
    private void fillCardExtraInfo(List<Card> cards) {
        fillUserInfo(cards);
        fillCommentCount(cards);
    }

    /**
     * 发布一张新卡片
     * 如果传入了 userId 且用户存在，则以该用户身份发布；
     * 否则使用系统中唯一的"匿名用户"发布（不存在则自动创建）
     *
     * @param content  卡片文本内容
     * @param nickname 昵称（未登录时会被忽略，统一使用匿名用户）
     * @param userId   发布者的用户ID，为 null 表示未登录
     * @return 创建好的卡片对象（包含用户信息和初始评论数 0）
     */
    public Card publish(String content, String nickname, Long userId) {
        User user = null;
        if (userId != null) {
            user = userMapper.selectById(userId);
        }
        if (user == null) {
            // 未登录时复用唯一的匿名用户，不存在则创建
            user = userMapper.selectOne(new QueryWrapper<User>()
                    .eq("nickname", "匿名用户"));
            if (user == null) {
                user = new User();
                user.setNickname("匿名用户");
                userMapper.insert(user);
            }
        }

        Card card = new Card();
        card.setUserId(user.getId());
        card.setContent(content);
        card.setLikesCount(0);
        save(card);

        // 设置关联信息用于返回给前端
        card.setUser(user);
        card.setCommentCount(0); // 新卡片评论数为 0
        return card;
    }

    /**
     * 给卡片点赞，使用 SQL 原子操作 likes_count + 1 防止并发问题
     *
     * @param cardId 要点赞的卡片ID
     */
    public void likeCard(Long cardId) {
        lambdaUpdate()
                .eq(Card::getId, cardId)
                .setSql("likes_count = likes_count + 1")
                .update();
    }

    /**
     * 随机获取卡片列表（用于首页"换一批"功能）
     * 使用数据库的 RAND() 函数随机排序，适合数据量不大的场景
     *
     * @param limit 返回的卡片数量上限
     * @return 随机排序的卡片列表（已填充用户信息和评论数量）
     */
    public List<Card> randomCards(int limit) {
        List<Card> cards = baseMapper.selectRandom(limit);
        fillCardExtraInfo(cards);
        return cards;
    }

    /**
     * 分页查询指定用户发布的卡片（用于"我的"标签页）
     *
     * @param userId 用户ID
     * @param page   当前页码
     * @param size   每页条数
     * @return 分页结果
     */
    public IPage<Card> listByUserId(Long userId, int page, int size) {
        Page<Card> p = new Page<>(page, size);
        IPage<Card> cards = lambdaQuery()
                .eq(Card::getUserId, userId)
                .orderByDesc(Card::getCreatedAt)
                .page(p);
        fillCardExtraInfo(cards.getRecords());
        return cards;
    }

    /**
     * 删除卡片（仅允许卡片的发布者本人操作）
     * 删除时会同时删除该卡片下的所有评论，保持数据一致性
     *
     * @param cardId 要删除的卡片ID
     * @param userId 当前操作者的用户ID，用于权限校验
     * @return true 删除成功，false 卡片不存在或无权限
     */
    @Transactional
    public boolean deleteCard(Long cardId, Long userId) {
        // 查询卡片是否存在
        Card card = getById(cardId);
        if (card == null) {
            return false;
        }

        // 权限校验：只有卡片的发布者才能删除自己的卡片
        if (!card.getUserId().equals(userId)) {
            return false;
        }

        // 先删除该卡片下的所有评论，避免产生孤儿数据
        commentMapper.delete(new QueryWrapper<Comment>().eq("card_id", cardId));

        // 删除该卡片的所有追记录
        cardFollowMapper.delete(new QueryWrapper<CardFollow>().eq("card_id", cardId));

        // 再删除卡片本身
        removeById(cardId);
        return true;
    }

    /**
     * 追/取消追卡片（toggle）
     * 已追则取消，未追则新增
     *
     * @return true 表示追上了，false 表示取消追了
     */
    public boolean toggleFollow(Long userId, Long cardId) {
        CardFollow existing = cardFollowMapper.selectOne(
                new QueryWrapper<CardFollow>()
                        .eq("user_id", userId)
                        .eq("card_id", cardId)
        );
        if (existing != null) {
            cardFollowMapper.deleteById(existing.getId());
            return false;
        } else {
            CardFollow follow = new CardFollow();
            follow.setUserId(userId);
            follow.setCardId(cardId);
            cardFollowMapper.insert(follow);
            return true;
        }
    }

    /**
     * 查询用户是否追了某些卡片（批量）
     *
     * @return 该用户已追的卡片ID集合
     */
    public Set<Long> getFollowedCardIds(Long userId, Set<Long> cardIds) {
        if (userId == null || cardIds.isEmpty()) return Collections.emptySet();
        List<CardFollow> follows = cardFollowMapper.selectList(
                new QueryWrapper<CardFollow>()
                        .eq("user_id", userId)
                        .in("card_id", cardIds)
        );
        return follows.stream().map(CardFollow::getCardId).collect(Collectors.toSet());
    }

    /**
     * 分页查询用户追的卡片列表
     */
    public IPage<Card> listFollowedCards(Long userId, int page, int size) {
        // 先查追的 card_id 列表
        List<CardFollow> follows = cardFollowMapper.selectList(
                new QueryWrapper<CardFollow>()
                        .eq("user_id", userId)
                        .orderByDesc("created_at")
        );
        if (follows.isEmpty()) {
            Page<Card> empty = new Page<>(page, size);
            empty.setRecords(Collections.emptyList());
            empty.setTotal(0);
            return empty;
        }

        List<Long> cardIds = follows.stream()
                .map(CardFollow::getCardId)
                .collect(Collectors.toList());

        // 手动分页
        int total = cardIds.size();
        int fromIndex = (page - 1) * size;
        if (fromIndex >= total) {
            Page<Card> empty = new Page<>(page, size);
            empty.setRecords(Collections.emptyList());
            empty.setTotal(total);
            return empty;
        }
        int toIndex = Math.min(fromIndex + size, total);
        List<Long> pageIds = cardIds.subList(fromIndex, toIndex);

        List<Card> cards = listByIds(pageIds);
        // 保持按追的时间排序
        Map<Long, Integer> orderMap = new java.util.HashMap<>();
        for (int i = 0; i < pageIds.size(); i++) {
            orderMap.put(pageIds.get(i), i);
        }
        cards.sort((a, b) -> orderMap.getOrDefault(a.getId(), 0) - orderMap.getOrDefault(b.getId(), 0));

        fillCardExtraInfo(cards);

        Page<Card> result = new Page<>(page, size);
        result.setRecords(cards);
        result.setTotal(total);
        return result;
    }
}
