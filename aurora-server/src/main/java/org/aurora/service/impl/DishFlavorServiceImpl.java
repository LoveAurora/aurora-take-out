package org.aurora.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.aurora.entity.DishFlavor;
import org.aurora.mapper.DishFlavorMapper;
import org.aurora.service.DishFlavorService;
import org.springframework.stereotype.Service;

/**
 * 菜品口味关系表(DishFlavor)表服务实现类
 *
 * @author Aurora
 * @since 2024-04-17 15:24:45
 */
@Service("dishFlavorService")
public class DishFlavorServiceImpl extends ServiceImpl<DishFlavorMapper, DishFlavor> implements DishFlavorService {

}

