package org.aurora.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.aurora.constant.MessageConstant;
import org.aurora.constant.StatusConstant;
import org.aurora.dto.DishDTO;
import org.aurora.dto.DishPageQueryDTO;
import org.aurora.entity.Dish;
import org.aurora.entity.DishFlavor;
import org.aurora.entity.Setmeal;
import org.aurora.entity.SetmealDish;
import org.aurora.exception.DeletionNotAllowedException;
import org.aurora.mapper.DishMapper;
import org.aurora.result.PageResult;
import org.aurora.service.DishFlavorService;
import org.aurora.service.DishService;
import org.aurora.service.SetmealDishService;
import org.aurora.service.SetmealService;
import org.aurora.utils.BeanCopyUtils;
import org.aurora.vo.DishVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 菜品(Dish)表服务实现类
 *
 * @author Aurora
 * @since 2024-04-17 15:24:45
 */
@Service("dishService")
@Slf4j
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    @Autowired
    private DishFlavorService dishFlavorService;
    @Autowired
    private SetmealDishService setmealDishService;
    @Autowired
    private SetmealService setmealService;


    @Override
    public void addDish(DishDTO dishDTO) {

        Dish dish = BeanCopyUtils.copyBean(dishDTO, Dish.class);
        save(dish);
        List<DishFlavor> flavors = dishDTO.getFlavors().stream()
                .peek(dishFlavor -> dishFlavor.setDishId(dish.getId())).collect(Collectors.toList());
        dishFlavorService.saveBatch(flavors);
    }

    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        LambdaQueryWrapper<Dish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Objects.nonNull(dishPageQueryDTO.getCategoryId()), Dish::getCategoryId, dishPageQueryDTO.getCategoryId());
        lambdaQueryWrapper.eq(Objects.nonNull(dishPageQueryDTO.getStatus()), Dish::getStatus, dishPageQueryDTO.getStatus());
        lambdaQueryWrapper.like(StringUtils.isNotEmpty(dishPageQueryDTO.getName()), Dish::getName, dishPageQueryDTO.getName());
        Page<Dish> page = new Page<>(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        page(page, lambdaQueryWrapper);

        return new PageResult(page.getRecords(), page.getTotal());
    }

    @Override
    public void deleteDish(List<Long> ids) {
        LambdaQueryWrapper<DishFlavor> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<SetmealDish> queryWrapperSetmealDish = new LambdaQueryWrapper<>();

        for (Long id : ids) {
            Dish dish = getById(id);
            queryWrapperSetmealDish.eq(SetmealDish::getDishId, id);
            if (setmealDishService.count(queryWrapperSetmealDish) > 0) {
                //当前菜品被套餐关联，不能删除
                throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
            }
            if (dish.getStatus().equals(StatusConstant.ENABLE)) {
                //当前菜品处于起售中，不能删除
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            } else {
                lambdaQueryWrapper.eq(DishFlavor::getDishId, id);
                dishFlavorService.remove(lambdaQueryWrapper);
                removeById(id);
            }
        }
    }

    @Override
    public DishDTO getDishById(Long id) {
        Dish dish = getById(id);
        LambdaQueryWrapper<DishFlavor> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        if (Objects.nonNull(dish)) {
            List<DishFlavor> flavors = dishFlavorService.list(lambdaQueryWrapper.eq(DishFlavor::getDishId, id));
            DishDTO dishDTO = BeanCopyUtils.copyBean(dish, DishDTO.class);
            dishDTO.setFlavors(flavors);
            return dishDTO;
        }
        return null;
    }

    @Override
    public void updateDish(DishDTO dishDTO) {

        Dish dish = BeanCopyUtils.copyBean(dishDTO, Dish.class);
        updateById(dish);
        List<DishFlavor> flavors = dishDTO.getFlavors().stream()
                .peek(dishFlavor -> dishFlavor.setDishId(dish.getId())).collect(Collectors.toList());
        LambdaQueryWrapper<DishFlavor> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(DishFlavor::getDishId, dish.getId());
        dishFlavorService.remove(lambdaQueryWrapper);
        dishFlavorService.saveBatch(flavors);
    }

    @Override
    @Transactional
    public void updateStatus(Integer status, Long id) {
        // 更新菜品状态
        LambdaUpdateWrapper<Dish> dishUpdateWrapper = new LambdaUpdateWrapper<>();
        dishUpdateWrapper.eq(Dish::getId, id).set(Dish::getStatus, status);
        update(dishUpdateWrapper);

        // 查询所有包含这个菜品的套餐
        LambdaQueryWrapper<SetmealDish> setmealDishWrapper = new LambdaQueryWrapper<>();
        setmealDishWrapper.eq(SetmealDish::getDishId, id);
        List<SetmealDish> setmealDishes = setmealDishService.list(setmealDishWrapper);

        if (!setmealDishes.isEmpty() && status.equals(StatusConstant.DISABLE)) {
            // 获取所有套餐的ID
            List<Long> setmealIds = setmealDishes.stream()
                    .map(SetmealDish::getSetmealId)
                    .distinct() // 避免重复ID
                    .collect(Collectors.toList());
            // 批量更新套餐状态
            LambdaUpdateWrapper<Setmeal> setmealUpdateWrapper = new LambdaUpdateWrapper<>();
            setmealUpdateWrapper.in(Setmeal::getId, setmealIds)
                    .set(Setmeal::getStatus, status);
            setmealService.update(setmealUpdateWrapper);
        }

    }

    @Override
    public List<DishVO> listbyCategoryId(Long categoryId) {
        LambdaQueryWrapper<Dish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Objects.nonNull(categoryId), Dish::getCategoryId, categoryId);
        List<Dish> dishes = list(lambdaQueryWrapper);
        log.info("dish:{}", dishes);
        LambdaQueryWrapper<DishFlavor> dishFlavorQueryWrapper = new LambdaQueryWrapper<>();
        if (Objects.nonNull(dishes)) {
            List<DishVO> dishVOS = BeanCopyUtils.copyBeanList(dishes, DishVO.class);
            for (DishVO dishVO : dishVOS) {
                dishFlavorQueryWrapper.eq(DishFlavor::getDishId, dishVO.getId());
                dishVO.setFlavors(dishFlavorService.list(dishFlavorQueryWrapper));
            }
            return dishVOS;
        }
        return null;
    }
}

