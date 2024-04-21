package org.aurora.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.aurora.constant.MessageConstant;
import org.aurora.constant.StatusConstant;
import org.aurora.dto.SetmealDTO;
import org.aurora.dto.SetmealPageQueryDTO;
import org.aurora.entity.Dish;
import org.aurora.entity.Setmeal;
import org.aurora.entity.SetmealDish;
import org.aurora.exception.SetmealEnableFailedException;
import org.aurora.mapper.SetmealMapper;
import org.aurora.result.PageResult;
import org.aurora.service.DishService;
import org.aurora.service.SetmealDishService;
import org.aurora.service.SetmealService;
import org.aurora.utils.BeanCopyUtils;
import org.aurora.vo.DishItemVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 套餐(Setmeal)表服务实现类
 *
 * @author Aurora
 * @since 2024-04-17 15:26:13
 */
@Service("setmealService")
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {
    @Autowired
    private SetmealDishService setmealDishService;
    @Autowired
    private DishService dishService;

    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        LambdaQueryWrapper<Setmeal> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Objects.nonNull(setmealPageQueryDTO.getCategoryId()), Setmeal::getCategoryId, setmealPageQueryDTO.getCategoryId());
        lambdaQueryWrapper.eq(Objects.nonNull(setmealPageQueryDTO.getStatus()), Setmeal::getStatus, setmealPageQueryDTO.getStatus());
        lambdaQueryWrapper.like(StringUtils.hasText(setmealPageQueryDTO.getName()), Setmeal::getName, setmealPageQueryDTO.getName());
        Page<Setmeal> page = new Page<>();
        page.setCurrent(setmealPageQueryDTO.getPage());
        page.setSize(setmealPageQueryDTO.getPageSize());
        page(page, lambdaQueryWrapper);
        return new PageResult(page.getRecords(), page.getTotal());
    }

    //TODO 如果套餐份数卖完了，会自动变为停售吗？
    @Override
    @Transactional()
    public void updateStatus(Integer status, Long id) {
        // 如果要启售套餐，需要确保所有菜品都已启售
        if (StatusConstant.ENABLE.equals(status)) {
            // 查询套餐中的所有菜品
            LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SetmealDish::getSetmealId, id);
            List<SetmealDish> setmealDishes = setmealDishService.list(queryWrapper);

            for (SetmealDish setmealDish : setmealDishes) {
                // 检查每个菜品的状态
                Dish dish = dishService.getById(setmealDish.getDishId());
                if (dish == null || StatusConstant.DISABLE.equals(dish.getStatus())) {
                    // 如果任何一个菜品未启售，返回false表示不能更新套餐状态
                    throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                }
            }
        }
        // 更新套餐状态
        LambdaUpdateWrapper<Setmeal> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Setmeal::getId, id)
                .set(Setmeal::getStatus, status);
        update(null, updateWrapper);
    }

    @Override
    public void updateMeal(SetmealDTO setmealDTO) {
        Setmeal setmeal = BeanCopyUtils.copyBean(setmealDTO, Setmeal.class);
        updateById(setmeal);
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        LambdaQueryWrapper<SetmealDish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(SetmealDish::getSetmealId, setmeal.getId());
        setmealDishService.remove(lambdaQueryWrapper);
        setmealDishes = setmealDishes.stream().peek(setmealDish -> setmealDish.setSetmealId(setmeal.getId())).collect(Collectors.toList());
        setmealDishService.saveBatch(setmealDishes);
    }

    @Override
    public void addMeal(SetmealDTO setmealDTO) {
        Setmeal setmeal = BeanCopyUtils.copyBean(setmealDTO, Setmeal.class);
        save(setmeal);
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes()
                .stream()
                .peek(setmealDish -> setmealDish.setSetmealId(setmeal.getId())).collect(Collectors.toList());

        setmealDishService.saveBatch(setmealDishes);
    }

    @Override
    public void deleteMeals(List<Long> ids) {
        LambdaQueryWrapper<SetmealDish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(SetmealDish::getSetmealId, ids);
        setmealDishService.remove(lambdaQueryWrapper);
        removeByIds(ids);
    }

    @Override
    public SetmealDTO getDishById(Long id) {
        Setmeal setmeal = getById(id);
        if (Objects.nonNull(setmeal)) {
            SetmealDTO setmealDTO = BeanCopyUtils.copyBean(setmeal, SetmealDTO.class);
            LambdaQueryWrapper<SetmealDish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(SetmealDish::getSetmealId, id);
            List<SetmealDish> list = setmealDishService.list(lambdaQueryWrapper);
            setmealDTO.setSetmealDishes(list);
            return setmealDTO;
        }
        return null;
    }

    @Override
    public List<Setmeal> getSetMealList(Long categoryId) {
        if (Objects.nonNull(categoryId)) {
            LambdaQueryWrapper<Setmeal> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(Setmeal::getCategoryId, categoryId);
            lambdaQueryWrapper.eq(Setmeal::getStatus, 1);
            return list(lambdaQueryWrapper);
        }
        return null;
    }

    @Override
    public List<DishItemVO> getDishItemById(Long id) {
        if (Objects.nonNull(id)) {
            LambdaQueryWrapper<SetmealDish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(SetmealDish::getSetmealId, id);
            List<SetmealDish> setmealDishes = setmealDishService.list(lambdaQueryWrapper);
            if (CollectionUtils.isNotEmpty(setmealDishes)) {
                List<Long> dishIds = setmealDishes.stream().map(SetmealDish::getDishId).collect(Collectors.toList());
                List<Dish> dishs = dishService.listByIds(dishIds);
                List<DishItemVO> dishItemVOS = BeanCopyUtils.copyBeanList(dishs, DishItemVO.class);
                return dishItemVOS;
            }
        }
        return null;
    }
}

