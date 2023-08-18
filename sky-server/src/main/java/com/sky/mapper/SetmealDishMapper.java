package com.sky.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SetmealDishMapper {
    /**
     * 根据菜品ID查询套餐ID
     *
     * @param dishIds
     * @return
     */

    List<Long> getSetmealIdsByDishId(List<Long> dishIds);
}
