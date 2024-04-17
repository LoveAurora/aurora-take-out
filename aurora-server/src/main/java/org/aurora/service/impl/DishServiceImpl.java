package org.aurora.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.aurora.entity.Dish;
import org.aurora.mapper.DishMapper;
import org.aurora.service.DishService;
import org.springframework.stereotype.Service;

/**
 * 菜品(Dish)表服务实现类
 *
 * @author Aurora
 * @since 2024-04-17 15:24:45
 */
@Service("dishService")
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

}

