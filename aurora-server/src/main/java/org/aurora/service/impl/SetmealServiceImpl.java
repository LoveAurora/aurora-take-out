package org.aurora.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.aurora.entity.Setmeal;
import org.aurora.mapper.SetmealMapper;
import org.aurora.service.SetmealService;
import org.springframework.stereotype.Service;

/**
 * 套餐(Setmeal)表服务实现类
 *
 * @author Aurora
 * @since 2024-04-17 15:26:13
 */
@Service("setmealService")
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {

}

