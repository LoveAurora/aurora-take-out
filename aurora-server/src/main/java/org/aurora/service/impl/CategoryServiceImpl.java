package org.aurora.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.aurora.constant.MessageConstant;
import org.aurora.constant.StatusConstant;
import org.aurora.dto.CategoryDTO;
import org.aurora.dto.CategoryPageQueryDTO;
import org.aurora.entity.Category;
import org.aurora.entity.Dish;
import org.aurora.entity.Setmeal;
import org.aurora.exception.BaseException;
import org.aurora.exception.DeletionNotAllowedException;
import org.aurora.mapper.CategoryMapper;
import org.aurora.result.PageResult;
import org.aurora.service.CategoryService;
import org.aurora.service.DishService;
import org.aurora.utils.BeanCopyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 菜品及套餐分类(Category)表服务实现类
 *
 * @author Aurora
 * @since 2024-04-17 15:24:45
 */
@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {
    @Autowired
    @Lazy
    private DishService dishService;
    @Autowired
    @Lazy
    private SetmealServiceImpl setmealService;


    // 添加分类
    @Override
    public void addCategory(CategoryDTO categoryDTO) {
        // 将DTO对象转换为实体对象
        Category category = BeanCopyUtils.copyBean(categoryDTO, Category.class);
        // 设置状态为禁用
        category.setStatus(StatusConstant.DISABLE);
        // 保存分类
        save(category);
    }

    // 分页查询
    @Override
    public PageResult pageQuery(CategoryPageQueryDTO categoryPageQueryDTO) {
        // 检查页码和页面大小是否有效
        if (categoryPageQueryDTO.getPage() == 0 || categoryPageQueryDTO.getPageSize() == 0) {
            return null;
        }
        // 创建查询条件
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(categoryPageQueryDTO.getType() != null, Category::getType, categoryPageQueryDTO.getType());
        queryWrapper.like(StringUtils.hasText(categoryPageQueryDTO.getName()), Category::getName, categoryPageQueryDTO.getName());
        // 创建分页对象
        Page<Category> page = new Page<>();
        page.setSize(categoryPageQueryDTO.getPageSize());
        page.setCurrent(categoryPageQueryDTO.getPage());
        // 执行分页查询
        page(page, queryWrapper);
        return new PageResult(page.getRecords(), page.getTotal());
    }

    // 启动或停止分类
    @Override
    public void startOrStop(Integer status, Long id) {

        Category category = getById(id);
        if (category == null) {
            // 处理未查询到记录的情况，例如抛出异常或返回错误信息
            throw new BaseException("没有该分类: " + id);
        }
        // 设置新的状态
        category.setStatus(status);
        // 更新分类
        updateById(category);
    }

    // 根据类型列出分类
    @Override
    public List<Category> list(Integer type) {
        // 创建查询条件
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Category::getType, type);
        // 查询并返回列表
        List<Category> list = list(queryWrapper);
        if (list == null || list.isEmpty()) {
            // 处理未查询到记录的情况，例如抛出异常或返回错误信息
            throw new BaseException("没有该类的分类菜单: " + type);
        }
        return list;
    }

    // 更新分类
    @Override
    public void updateCategory(CategoryDTO categoryDTO) {
        // 将DTO对象转换为实体对象
        Category category = BeanCopyUtils.copyBean(categoryDTO, Category.class);
        // 更新分类
        updateById(category);
    }

    @Override
    public void removeCategory(Long id) {
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Dish::getCategoryId, id);
        List<Dish> list = dishService.list(queryWrapper);

        LambdaQueryWrapper<Setmeal> setmealQueryWrapper = new LambdaQueryWrapper<>();
        setmealQueryWrapper.eq(Setmeal::getCategoryId, id);
        List<Setmeal> setmealList = setmealService.list(setmealQueryWrapper);
        if (list != null && !list.isEmpty()) {
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_DISH);
        } else if (setmealList != null && !setmealList.isEmpty()) {
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_SETMEAL);
        } else {
            removeById(id);
        }
    }

    @Override
    public List<Category> listCategory(Integer type) {
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();

        if (type == null) {
            queryWrapper.eq(Category::getStatus, StatusConstant.ENABLE)
                    .orderByAsc(Category::getSort)
                    .orderByDesc(Category::getCreateTime);
            return list(queryWrapper);
        } else {
            queryWrapper.eq(Category::getType, type);
        }
        return list(queryWrapper);
    }
}