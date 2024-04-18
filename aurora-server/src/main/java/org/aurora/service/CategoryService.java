package org.aurora.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aurora.dto.CategoryDTO;
import org.aurora.dto.CategoryPageQueryDTO;
import org.aurora.entity.Category;
import org.aurora.result.PageResult;

import java.util.List;

/**
 * 菜品及套餐分类(Category)表服务接口
 *
 * @author Aurora
 * @since 2024-04-17 15:24:45
 */
public interface CategoryService extends IService<Category> {

    void addCategory(CategoryDTO categoryDTO);

    PageResult pageQuery(CategoryPageQueryDTO categoryPageQueryDTO);

    void startOrStop(Integer status, Long id);

    List<Category> list(Integer type);

    void updateCategory(CategoryDTO categoryDTO);

    void removeCategory(Long id);
}

