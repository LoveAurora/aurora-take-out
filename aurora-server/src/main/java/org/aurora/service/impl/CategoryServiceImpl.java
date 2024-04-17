package org.aurora.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.aurora.entity.Category;
import org.aurora.mapper.CategoryMapper;
import org.aurora.service.CategoryService;
import org.springframework.stereotype.Service;

/**
 * 菜品及套餐分类(Category)表服务实现类
 *
 * @author Aurora
 * @since 2024-04-17 15:24:45
 */
@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

}

