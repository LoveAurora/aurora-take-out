package org.aurora.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aurora.dto.DishDTO;
import org.aurora.dto.DishPageQueryDTO;
import org.aurora.entity.Dish;
import org.aurora.result.PageResult;
import org.aurora.vo.DishVO;

import java.util.List;

/**
 * 菜品(Dish)表服务接口
 *
 * @author Aurora
 * @since 2024-04-17 15:24:45
 */
public interface DishService extends IService<Dish> {

    void addDish(DishDTO dishDTO);

    PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);

    void deleteDish(List<Long> ids);

    DishDTO getDishById(Long id);


    void updateDish(DishDTO dishDTO);


    List<DishVO> listbyCategoryId(Long categoryId);

    void updateStatus(Integer status, Long id);
}

