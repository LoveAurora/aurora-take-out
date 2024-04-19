package org.aurora.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.aurora.dto.SetmealDTO;
import org.aurora.dto.SetmealPageQueryDTO;
import org.aurora.entity.Setmeal;
import org.aurora.entity.SetmealDish;
import org.aurora.mapper.SetmealMapper;
import org.aurora.result.PageResult;
import org.aurora.service.SetmealDishService;
import org.aurora.service.SetmealService;
import org.aurora.utils.BeanCopyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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

    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        LambdaQueryWrapper<Setmeal> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Objects.nonNull(setmealPageQueryDTO.getCategoryId()), Setmeal::getCategoryId, setmealPageQueryDTO.getCategoryId());
        lambdaQueryWrapper.eq(Objects.nonNull(setmealPageQueryDTO.getStatus()), Setmeal::getStatus, setmealPageQueryDTO.getStatus());
        lambdaQueryWrapper.eq(StringUtils.hasText(setmealPageQueryDTO.getName()), Setmeal::getName, setmealPageQueryDTO.getName());
        Page<Setmeal> page = new Page<>();
        page.setCurrent(setmealPageQueryDTO.getPage());
        page.setSize(setmealPageQueryDTO.getPageSize());
        page(page, lambdaQueryWrapper);
        return new PageResult(page.getRecords(), page.getTotal());
    }

    @Override
    public void updateMeal(SetmealDTO setmealDTO) {

    }

    @Override
    public void addMeal(SetmealDTO setmealDTO) {
        Setmeal setmeal = BeanCopyUtils.copyBean(setmealDTO, Setmeal.class);
        save(setmeal);
        setmeal.getId();
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes()
                .stream()
                .map(setmealDish -> {
                    setmealDish.setSetmealId(setmeal.getId());
                    return setmealDish;
                }).collect(Collectors.toList());

        setmealDishService.saveBatch(setmealDishes);
    }
}

