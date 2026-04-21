package com.cardword.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cardword.entity.Card;
import com.cardword.entity.CardFollow;
import com.cardword.entity.CardTag;
import com.cardword.entity.Comment;
import com.cardword.entity.Tag;
import com.cardword.entity.User;
import com.cardword.mapper.CardFollowMapper;
import com.cardword.mapper.CardMapper;
import com.cardword.mapper.CardTagMapper;
import com.cardword.mapper.CommentMapper;
import com.cardword.mapper.TagMapper;
import com.cardword.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CardService extends ServiceImpl<CardMapper, Card> {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private CardFollowMapper cardFollowMapper;

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private CardTagMapper cardTagMapper;

    /**
     * 分页查询所有卡片列表
     * 按创建时间倒序排列，同时填充每张卡片的用户信息和评论数量
     *
     * @param page 当前页码（从1开始）
     * @param size 每页条数
     * @return 分页结果，包含卡片列表及分页元数据
     */
    public IPage<Card> listCards(int page, int size, Long viewerId) {
        Page<Card> p = new Page<>(page, size);
        IPage<Card> cards = lambdaQuery()
                .orderByDesc(Card::getCreatedAt)
                .page(p);
        fillCardExtraInfo(cards.getRecords());
        applyViewerVisibility(cards.getRecords(), viewerId);
        return cards;
    }

    /**
     * 批量填充卡片的用户信息
     * 直接从卡片的 username 字段读取，无需关联查询用户表
     * 提升查询性能
     *
     * @param cards 需要填充用户信息的卡片列表
     */
    private void fillUserInfo(List<Card> cards) {
        // 无需处理，username 字段已经直接从数据库读取
        // 前端可以直接使用 card.username 获取用户名
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

    private void fillTags(List<Card> cards) {
        if (cards == null || cards.isEmpty()) return;

        Set<Long> cardIds = cards.stream()
                .map(Card::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (cardIds.isEmpty()) return;

        List<CardTag> relations = cardTagMapper.selectList(
                new QueryWrapper<CardTag>()
                        .in("card_id", cardIds)
                        .select("card_id", "tag_id")
        );
        if (relations.isEmpty()) {
            for (Card card : cards) {
                card.setTags(Collections.emptyList());
            }
            return;
        }

        Set<Long> tagIds = relations.stream()
                .map(CardTag::getTagId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (tagIds.isEmpty()) {
            for (Card card : cards) {
                card.setTags(Collections.emptyList());
            }
            return;
        }

        List<Tag> tags = tagMapper.selectList(
                new QueryWrapper<Tag>()
                        .in("id", tagIds)
                        .select("id", "name")
        );
        Map<Long, Tag> tagById = tags.stream().collect(Collectors.toMap(Tag::getId, t -> t));

        Map<Long, List<Tag>> tagsByCardId = new HashMap<>();
        for (CardTag rel : relations) {
            Tag tag = tagById.get(rel.getTagId());
            if (tag == null) continue;
            tagsByCardId.computeIfAbsent(rel.getCardId(), k -> new ArrayList<>()).add(tag);
        }

        for (Card card : cards) {
            card.setTags(tagsByCardId.getOrDefault(card.getId(), Collections.emptyList()));
        }
    }

    /**
     * 统一填充卡片的所有额外信息（用户信息 + 评论数量 + 标签）
     * 所有查询卡片列表的方法都应调用此方法，确保返回数据的一致性
     *
     * @param cards 需要填充额外信息的卡片列表
     */
    private void fillCardExtraInfo(List<Card> cards) {
        fillUserInfo(cards);
        fillCommentCount(cards);
        fillTags(cards);
    }

    private void applyViewerVisibility(List<Card> cards, Long viewerId) {
        if (cards == null) return;
        for (Card card : cards) {
            boolean isOwner = viewerId != null && card.getUserId() != null && card.getUserId().equals(viewerId);
            card.setIsOwner(isOwner);
            // 如果是匿名卡片且不是所有者，隐藏用户信息
            if (card.getIsAnonymous() != null && card.getIsAnonymous() == 1 && !isOwner) {
                card.setUserId(null);
                card.setUsername("");
            }
        }
    }

    /**
     * 发布一张新卡片
     * 必须登录才能发布卡片，userId 不能为 null
     */
    @Transactional
    public Card publish(String content, String nickname, Long userId, String imageUrl, Integer isAnonymous, List<String> tagNames) {
        // 必须登录才能发布
        if (userId == null) {
            throw new IllegalArgumentException("必须登录才能发布卡片");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        Card card = new Card();
        card.setUserId(userId);
        card.setUsername(user.getNickname());
        card.setContent(content);
        card.setImageUrl(imageUrl);
        card.setIsAnonymous(isAnonymous);
        card.setLikesCount(0);
        save(card);

        List<Tag> attachedTags = attachTags(card.getId(), tagNames);

        // 发布卡片奖励 10 经验值
        userService.addExp(userId, 10);

        // 设置附加信息用于返回给前端
        card.setCommentCount(0); // 新卡片评论数为 0
        card.setIsOwner(true);
        card.setTags(attachedTags);
        return card;
    }

    private List<Tag> attachTags(Long cardId, List<String> tagNames) {
        if (cardId == null || tagNames == null || tagNames.isEmpty()) return Collections.emptyList();

        List<Tag> result = new ArrayList<>();
        for (String raw : tagNames) {
            if (raw == null) continue;
            String name = raw.trim();
            if (name.isEmpty()) continue;

            Tag tag = tagMapper.selectOne(new QueryWrapper<Tag>().eq("name", name));
            if (tag == null) {
                Tag toInsert = new Tag();
                toInsert.setName(name);
                toInsert.setUseCount(0);
                try {
                    tagMapper.insert(toInsert);
                    tag = toInsert;
                } catch (DuplicateKeyException ex) {
                    tag = tagMapper.selectOne(new QueryWrapper<Tag>().eq("name", name));
                }
            }
            if (tag == null || tag.getId() == null) continue;

            CardTag rel = new CardTag();
            rel.setCardId(cardId);
            rel.setTagId(tag.getId());
            try {
                cardTagMapper.insert(rel);
                tagMapper.update(null,
                    new UpdateWrapper<Tag>()
                        .eq("id", tag.getId())
                        .setSql("use_count = use_count + 1")
                );
            } catch (DuplicateKeyException ignore) {
            }

            result.add(tag);
        }
        return result;
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
     */
    public Map<String, Object> randomCards(int limit, List<Long> excludeIds, Long viewerId) {
        List<Long> normalizedExcludeIds = excludeIds == null ? Collections.emptyList() : excludeIds.stream()
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        List<Card> cards = baseMapper.selectRandom(limit, normalizedExcludeIds);
        boolean resetBlacklist = false;
        if (cards.size() < limit) {
            resetBlacklist = true;
            Set<Long> selectedIds = cards.stream().map(Card::getId).collect(Collectors.toSet());
            List<Card> fallbackCards = baseMapper.selectRandom(limit - cards.size(), new ArrayList<>(selectedIds));
            cards.addAll(fallbackCards);
        }
        fillCardExtraInfo(cards);
        applyViewerVisibility(cards, viewerId);

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("cards", cards);
        result.put("resetBlacklist", resetBlacklist);
        return result;
    }

    public IPage<Card> listByUserId(Long userId, int page, int size, Long viewerId) {
        Page<Card> p = new Page<>(page, size);
        IPage<Card> cards = lambdaQuery()
                .eq(Card::getUserId, userId)
                .orderByDesc(Card::getCreatedAt)
                .page(p);
        fillCardExtraInfo(cards.getRecords());
        applyViewerVisibility(cards.getRecords(), viewerId);
        return cards;
    }

    public IPage<Card> listByTagId(Long tagId, int page, int size, Long viewerId) {
        if (tagId == null) {
            Page<Card> empty = new Page<>(page, size);
            empty.setRecords(Collections.emptyList());
            empty.setTotal(0);
            return empty;
        }

        Page<CardTag> relPage = new Page<>(page, size);
        IPage<CardTag> rels = cardTagMapper.selectPage(
                relPage,
                new QueryWrapper<CardTag>()
                        .eq("tag_id", tagId)
                        .orderByDesc("created_at")
        );

        List<CardTag> relRecords = rels.getRecords();
        if (relRecords == null || relRecords.isEmpty()) {
            Page<Card> empty = new Page<>(page, size);
            empty.setRecords(Collections.emptyList());
            empty.setTotal(rels.getTotal());
            return empty;
        }

        List<Long> cardIds = relRecords.stream()
                .map(CardTag::getCardId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<Card> cards = listByIds(cardIds);
        Map<Long, Integer> orderMap = new HashMap<>();
        for (int i = 0; i < cardIds.size(); i++) {
            orderMap.put(cardIds.get(i), i);
        }
        cards.sort((a, b) -> orderMap.getOrDefault(a.getId(), 0) - orderMap.getOrDefault(b.getId(), 0));

        fillCardExtraInfo(cards);
        applyViewerVisibility(cards, viewerId);

        Page<Card> result = new Page<>(page, size);
        result.setRecords(cards);
        result.setTotal(rels.getTotal());
        return result;
    }

    @Transactional
    public boolean deleteCard(Long cardId, Long userId) {
        Card card = getById(cardId);
        if (card == null) {
            return false;
        }

        if (!card.getUserId().equals(userId)) {
            return false;
        }

        List<CardTag> cardTags = cardTagMapper.selectList(
                new QueryWrapper<CardTag>().eq("card_id", cardId).select("tag_id")
        );

        commentMapper.delete(new QueryWrapper<Comment>().eq("card_id", cardId));
        cardFollowMapper.delete(new QueryWrapper<CardFollow>().eq("card_id", cardId));
        cardTagMapper.delete(new QueryWrapper<CardTag>().eq("card_id", cardId));

        for (CardTag ct : cardTags) {
            tagMapper.update(null,
                new UpdateWrapper<Tag>()
                    .eq("id", ct.getTagId())
                    .gt("use_count", 0)
                    .setSql("use_count = use_count - 1")
            );
        }

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
    public IPage<Card> listFollowedCards(Long userId, int page, int size, Long viewerId) {
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
        applyViewerVisibility(cards, viewerId);

        Page<Card> result = new Page<>(page, size);
        result.setRecords(cards);
        result.setTotal(total);
        return result;
    }

    /**
     * 获取用户追的卡片ID列表
     */
    public List<Long> listFollowedCardIds(Long userId) {
        List<CardFollow> follows = cardFollowMapper.selectList(
                new QueryWrapper<CardFollow>()
                        .eq("user_id", userId)
                        .select("card_id")
        );
        return follows.stream()
                .map(CardFollow::getCardId)
                .collect(Collectors.toList());
    }
}
