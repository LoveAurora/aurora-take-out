package org.aurora.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aurora.dto.SetmealDTO;
import org.aurora.dto.SetmealPageQueryDTO;
import org.aurora.entity.Setmeal;
import org.aurora.result.PageResult;
import org.aurora.vo.DishItemVO;

import java.util.List;

/**
 * 套餐(Setmeal)表服务接口
 *
 * @author Aurora
 * @since 2024-04-17 15:25:23
 */
public interface SetmealService extends IService<Setmeal> {

    PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    void updateMeal(SetmealDTO setmealDTO);

    void addMeal(SetmealDTO setmealDTO);


    SetmealDTO getDishById(Long id);

    void deleteMeals(List<Long> ids);

    List<Setmeal> getSetMealList(Long categoryId);

    void updateStatus(Integer status, Long id);

    List<DishItemVO> getDishItemById(Long id);
}

