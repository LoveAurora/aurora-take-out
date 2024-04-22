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
import org.aurora.vo.SetmealVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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
    private final SetmealDishService setmealDishService;
    @Autowired
    @Lazy
    private CategoryServiceImpl categoryService;
    // 使用 @Lazy 注解来解决循环依赖的问题
    @Autowired
    @Lazy
    private DishService dishService;

    // 构造函数注入 SetmealDishService
    public SetmealServiceImpl(SetmealDishService setmealDishService) {
        this.setmealDishService = setmealDishService;
    }

    // 分页查询
    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        // 创建查询条件
        LambdaQueryWrapper<Setmeal> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        // 如果分类ID不为空，添加分类ID查询条件
        lambdaQueryWrapper.eq(Objects.nonNull(setmealPageQueryDTO.getCategoryId()), Setmeal::getCategoryId, setmealPageQueryDTO.getCategoryId());
        // 如果状态不为空，添加状态查询条件
        lambdaQueryWrapper.eq(Objects.nonNull(setmealPageQueryDTO.getStatus()), Setmeal::getStatus, setmealPageQueryDTO.getStatus());
        // 如果名称不为空，添加名称查询条件
        lambdaQueryWrapper.like(StringUtils.hasText(setmealPageQueryDTO.getName()), Setmeal::getName, setmealPageQueryDTO.getName());
        // 创建分页对象
        Page<Setmeal> page = new Page<>();
        // 设置当前页数
        page.setCurrent(setmealPageQueryDTO.getPage());
        // 设置每页的记录数
        page.setSize(setmealPageQueryDTO.getPageSize());
        // 执行分页查询
        page(page, lambdaQueryWrapper);
        List<SetmealVO> setmealVOS = BeanCopyUtils.copyBeanList(page.getRecords(), SetmealVO.class);
        List<SetmealVO> collect = setmealVOS.stream()
                .peek(setmealVO -> setmealVO.setCategoryName(categoryService.getById(setmealVO.getCategoryId()).getName()))
                .collect(Collectors.toList());
        // 返回分页结果，包括记录和总数
        // 总数为page.getTotal() 不能写 collect.size() collect是每一页的数量collect.size() 就定死了
        // 前端根据总数来分页的
        return new PageResult(collect, page.getTotal());
    }

    // 更新套餐状态
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

    // 更新套餐
    @Override
    public void updateMeal(SetmealDTO setmealDTO) {
        // 将 DTO 对象转换为实体对象
        Setmeal setmeal = BeanCopyUtils.copyBean(setmealDTO, Setmeal.class);
        // 更新套餐
        updateById(setmeal);
        // 获取套餐中的菜品列表
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        // 创建查询条件
        LambdaQueryWrapper<SetmealDish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(SetmealDish::getSetmealId, setmeal.getId());
        // 删除旧的菜品列表
        setmealDishService.remove(lambdaQueryWrapper);
        // 添加新的菜品列表
        setmealDishes = setmealDishes.stream().peek(setmealDish -> setmealDish.setSetmealId(setmeal.getId())).collect(Collectors.toList());
        setmealDishService.saveBatch(setmealDishes);
    }

    // 添加套餐
    @Override
    public void addMeal(SetmealDTO setmealDTO) {
        // 将 DTO 对象转换为实体对象
        Setmeal setmeal = BeanCopyUtils.copyBean(setmealDTO, Setmeal.class);
        // 保存套餐
        save(setmeal);
        // 获取套餐中的菜品列表
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes()
                .stream()
                .peek(setmealDish -> setmealDish.setSetmealId(setmeal.getId())).collect(Collectors.toList());
        // 保存菜品列表
        setmealDishService.saveBatch(setmealDishes);
    }

    // 删除套餐
    @Override
    @Transactional
    public void deleteMeals(List<Long> ids) {
        // 创建查询条件
        LambdaQueryWrapper<SetmealDish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(SetmealDish::getSetmealId, ids);
        // 删除套餐中的菜品
        setmealDishService.remove(lambdaQueryWrapper);
        // 删除套餐
        removeByIds(ids);
    }

    // 根据 ID 获取套餐
    @Override
    public SetmealDTO getDishById(Long id) {
        // 获取套餐
        Setmeal setmeal = getById(id);
        if (Objects.nonNull(setmeal)) {
            // 将实体对象转换为 DTO 对象
            SetmealDTO setmealDTO = BeanCopyUtils.copyBean(setmeal, SetmealDTO.class);
            // 创建查询条件
            LambdaQueryWrapper<SetmealDish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(SetmealDish::getSetmealId, id);
            // 获取套餐中的菜品列表
            List<SetmealDish> list = setmealDishService.list(lambdaQueryWrapper);
            // 设置菜品列表
            setmealDTO.setSetmealDishes(list);
            return setmealDTO;
        }
        return null;
    }

    // 根据分类 ID 获取套餐列表
    @Override
    public List<Setmeal> getSetMealList(Long categoryId) {
        if (Objects.nonNull(categoryId)) {
            // 创建查询条件
            LambdaQueryWrapper<Setmeal> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(Setmeal::getCategoryId, categoryId);
            lambdaQueryWrapper.eq(Setmeal::getStatus, 1);
            // 获取套餐列表
            return list(lambdaQueryWrapper);
        }
        return null;
    }

    // 根据 ID 获取菜品项
    @Override
    public List<DishItemVO> getDishItemById(Long id) {
        if (Objects.nonNull(id)) {
            // 创建查询条件
            LambdaQueryWrapper<SetmealDish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(SetmealDish::getSetmealId, id);
            // 获取套餐中的菜品列表
            List<SetmealDish> setmealDishes = setmealDishService.list(lambdaQueryWrapper);
            if (CollectionUtils.isNotEmpty(setmealDishes)) {
                // 获取菜品 ID 列表
                List<Long> dishIds = setmealDishes.stream().map(SetmealDish::getDishId).collect(Collectors.toList());
                // 获取菜品列表
                List<Dish> dishs = dishService.listByIds(dishIds);
                // 将实体对象列表转换为 VO 对象列表
                List<DishItemVO> dishItemVOS = BeanCopyUtils.copyBeanList(dishs, DishItemVO.class);
                return dishItemVOS;
            }
        }
        return null;
    }
}