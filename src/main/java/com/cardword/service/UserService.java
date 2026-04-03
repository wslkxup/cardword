package com.cardword.service;

import com.cardword.entity.User;
import com.cardword.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 用户服务类
 * 处理用户等级、经验值等相关业务逻辑
 */
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    /**
     * 计算从当前等级升级到下一级所需的经验值
     * 规则：0-1 级需要 100 经验，之后每级多加 50 经验
     *
     * @param level 当前等级
     * @return 升级到下一级所需的总经验值
     */
    public int getExpForNextLevel(int level) {
        int totalExp = 0;
        for (int i = 0; i <= level; i++) {
            totalExp += 100 + (i * 50);
        }
        return totalExp;
    }

    /**
     * 根据当前经验值计算用户等级
     *
     * @param exp 当前经验值
     * @return 计算得出的等级
     */
    public int calculateLevel(int exp) {
        int level = 0;
        int totalExp = 0;
        
        while (true) {
            int nextLevelExp = getExpForNextLevel(level);
            if (exp < nextLevelExp) {
                break;
            }
            level++;
        }
        
        return level;
    }

    /**
     * 计算当前等级已经获得的有效经验值（用于进度条显示）
     * 例如：升到 1 级需要 100 经验，用户有 120 经验，则显示 20/150
     *
     * @param exp 总经验值
     * @param level 当前等级
     * @return 当前等级已获得的有效经验值（从 0 开始计算）
     */
    public int getCurrentLevelExp(int exp, int level) {
        if (level == 0) {
            return exp;
        }
        // 上一级升级所需的总经验值
        int prevLevelTotalExp = getExpForNextLevel(level - 1);
        // 当前等级已经获得的经验值（从 0 开始）
        return exp - prevLevelTotalExp;
    }

    /**
     * 计算下一级所需的总经验值
     *
     * @param level 当前等级
     * @return 下一级所需的总经验值
     */
    public int getNextLevelExp(int level) {
        return getExpForNextLevel(level);
    }

    /**
     * 计算当前等级升级到下一级还需要的经验值
     *
     * @param exp 当前经验值
     * @param level 当前等级
     * @return 还需要的经验值
     */
    public int getExpNeeded(int exp, int level) {
        int nextLevelExp = getExpForNextLevel(level);
        return nextLevelExp - exp;
    }

    /**
     * 为用户增加经验值，并自动计算和更新等级
     *
     * @param userId 用户 ID
     * @param expToAdd 要增加的经验值
     * @return 增加后的总经验值和等级
     */
    public User addExp(Long userId, int expToAdd) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }

        // 初始化经验值和等级（如果为 null）
        if (user.getExp() == null) {
            user.setExp(0);
        }
        if (user.getLevel() == null) {
            user.setLevel(0);
        }

        // 增加经验值
        int newExp = user.getExp() + expToAdd;
        user.setExp(newExp);

        // 重新计算等级
        int newLevel = calculateLevel(newExp);
        user.setLevel(newLevel);

        // 更新到数据库
        userMapper.updateById(user);

        return user;
    }

    /**
     * 获取用户信息（包含经验值和等级）
     *
     * @param userId 用户 ID
     * @return 用户信息，包含经验值和等级
     */
    public User getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }

        // 初始化经验值和等级（如果为 null）
        if (user.getExp() == null) {
            user.setExp(0);
        }
        if (user.getLevel() == null) {
            user.setLevel(0);
        }

        return user;
    }
}
