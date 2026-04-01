package com.cardword.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cardword.entity.Card;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface CardMapper extends BaseMapper<Card> {

    @Select("SELECT * FROM card ORDER BY RAND() LIMIT #{limit}")
    List<Card> selectRandom(@Param("limit") int limit);
}
