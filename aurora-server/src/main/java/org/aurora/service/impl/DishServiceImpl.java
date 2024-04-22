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
import org.springframework.context.annotation.Lazy;
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

    private final DishFlavorService dishFlavorService;
    private final SetmealDishService setmealDishService;

    // 使用 @Lazy 注解来解决循环依赖的问题
    @Autowired
    @Lazy
    private SetmealService setmealService;

    @Autowired
    @Lazy
    private CategoryServiceImpl categoryService;

    // 构造函数注入 DishFlavorService 和 SetmealDishService
    public DishServiceImpl(DishFlavorService dishFlavorService, SetmealDishService setmealDishService) {
        this.dishFlavorService = dishFlavorService;
        this.setmealDishService = setmealDishService;
    }

    // 添加菜品
    @Override
    public void addDish(DishDTO dishDTO) {
        // 将 DTO 对象转换为实体对象
        Dish dish = BeanCopyUtils.copyBean(dishDTO, Dish.class);
        // 保存菜品
        save(dish);
        // 获取菜品的口味列表
        List<DishFlavor> flavors = dishDTO.getFlavors().stream()
                .peek(dishFlavor -> dishFlavor.setDishId(dish.getId())).collect(Collectors.toList());
        // 保存口味列表
        dishFlavorService.saveBatch(flavors);
    }

    // 分页查询
    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        // 创建查询条件
        LambdaQueryWrapper<Dish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Objects.nonNull(dishPageQueryDTO.getCategoryId()), Dish::getCategoryId, dishPageQueryDTO.getCategoryId());
        lambdaQueryWrapper.eq(Objects.nonNull(dishPageQueryDTO.getStatus()), Dish::getStatus, dishPageQueryDTO.getStatus());
        lambdaQueryWrapper.like(StringUtils.isNotEmpty(dishPageQueryDTO.getName()), Dish::getName, dishPageQueryDTO.getName());
        // 创建分页对象
        Page<Dish> page = new Page<>(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        // 执行分页查询
        page(page, lambdaQueryWrapper);

        List<DishVO> dishVOList = BeanCopyUtils.copyBeanList(page.getRecords(), DishVO.class);
        List<DishVO> collect = dishVOList.stream()
                .peek(dishVO -> dishVO.setCategoryName(categoryService.getById(dishVO.getCategoryId()).getName()))
                .collect(Collectors.toList());

        // 返回分页结果
        // 总数为page.getTotal() 不能写 collect.size() collect是每一页的数量collect.size() 就定死了
        // 前端根据总数来分页的
        return new PageResult(collect, page.getTotal());
    }

    // 删除菜品
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
                // 删除菜品的口味
                dishFlavorService.remove(lambdaQueryWrapper);
                // 删除菜品
                removeById(id);
            }
        }
    }

    // 根据 ID 获取菜品
    @Override
    public DishDTO getDishById(Long id) {
        // 获取菜品
        Dish dish = getById(id);
        LambdaQueryWrapper<DishFlavor> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        if (Objects.nonNull(dish)) {
            // 获取菜品的口味列表
            List<DishFlavor> flavors = dishFlavorService.list(lambdaQueryWrapper.eq(DishFlavor::getDishId, id));
            // 将实体对象转换为 DTO 对象
            DishDTO dishDTO = BeanCopyUtils.copyBean(dish, DishDTO.class);
            // 设置口味列表
            dishDTO.setFlavors(flavors);
            return dishDTO;
        }
        return null;
    }

    // 更新菜品
    @Override
    public void updateDish(DishDTO dishDTO) {
        // 将 DTO 对象转换为实体对象
        Dish dish = BeanCopyUtils.copyBean(dishDTO, Dish.class);
        // 更新菜品
        updateById(dish);
        // 获取菜品的口味列表
        List<DishFlavor> flavors = dishDTO.getFlavors().stream()
                .peek(dishFlavor -> dishFlavor.setDishId(dish.getId())).collect(Collectors.toList());
        LambdaQueryWrapper<DishFlavor> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(DishFlavor::getDishId, dish.getId());
        // 删除旧的口味列表
        dishFlavorService.remove(lambdaQueryWrapper);
        // 保存新的口味列表
        dishFlavorService.saveBatch(flavors);
    }

    // 更新菜品状态
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

    // 根据分类 ID 获取菜品列表
    @Override
    public List<DishVO> listbyCategoryId(Long categoryId) {
        LambdaQueryWrapper<Dish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Objects.nonNull(categoryId), Dish::getCategoryId, categoryId);
        // 获取菜品列表
        List<Dish> dishes = list(lambdaQueryWrapper);
        log.info("dish:{}", dishes);
        LambdaQueryWrapper<DishFlavor> dishFlavorQueryWrapper = new LambdaQueryWrapper<>();
        if (Objects.nonNull(dishes)) {
            // 将实体对象列表转换为 VO 对象列表
            List<DishVO> dishVOS = BeanCopyUtils.copyBeanList(dishes, DishVO.class);
            for (DishVO dishVO : dishVOS) {
                dishFlavorQueryWrapper.eq(DishFlavor::getDishId, dishVO.getId());
                // 获取菜品的口味列表
                dishVO.setFlavors(dishFlavorService.list(dishFlavorQueryWrapper));
            }
            return dishVOS;
        }
        return null;
    }
}