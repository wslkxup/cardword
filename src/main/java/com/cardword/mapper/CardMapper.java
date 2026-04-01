package com.cardword.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cardword.entity.Card;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface CardMapper extends BaseMapper<Card> {

    @Select({"<script>",
            "SELECT * FROM card",
            "<where>",
            "<if test='excludeIds != null and excludeIds.size > 0'>",
            "AND id NOT IN",
            "<foreach collection='excludeIds' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</if>",
            "</where>",
            "ORDER BY RAND() LIMIT #{limit}",
            "</script>"})
    List<Card> selectRandom(@Param("limit") int limit, @Param("excludeIds") List<Long> excludeIds);
}
