package org.aurora.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.aurora.dto.DishDTO;
import org.aurora.dto.DishPageQueryDTO;
import org.aurora.entity.Dish;
import org.aurora.entity.DishFlavor;
import org.aurora.mapper.DishMapper;
import org.aurora.result.PageResult;
import org.aurora.service.DishFlavorService;
import org.aurora.service.DishService;
import org.aurora.utils.BeanCopyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    @Autowired
    private DishFlavorService dishFlavorService;

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

        for (Long id : ids) {
            lambdaQueryWrapper.eq(DishFlavor::getDishId, id);
            dishFlavorService.remove(lambdaQueryWrapper);
            removeById(id);
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
    public List<DishDTO> getByCategoryId(Long categoryId) {
        LambdaQueryWrapper<Dish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<DishFlavor> dishFlavorQueryWrapper = new LambdaQueryWrapper<>();
        if (Objects.nonNull(categoryId)) {
            lambdaQueryWrapper.eq(Dish::getCategoryId, categoryId);
            List<Dish> dishes = list(lambdaQueryWrapper);
            List<DishDTO> collect = dishes.stream().map(dish -> {
                dishFlavorQueryWrapper.eq(DishFlavor::getDishId, dish.getId());
                List<DishFlavor> list = dishFlavorService.list(dishFlavorQueryWrapper);
                DishDTO dishDTO = BeanCopyUtils.copyBean(dish, DishDTO.class);
                dishDTO.setFlavors(list);
                return dishDTO;
            }).collect(Collectors.toList());
            return collect;
        }
        return null;
    }

}

