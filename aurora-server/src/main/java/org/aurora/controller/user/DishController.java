package org.aurora.controller.user;


import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.aurora.result.Result;
import org.aurora.service.DishService;
import org.aurora.utils.RedisCache;
import org.aurora.vo.DishVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 菜品(Dish)表控制层
 *
 * @author Aurora
 * @since 2024-04-17 15:24:45
 */
@RestController("userDishController")
@Slf4j
@RequestMapping("/user/dish")
public class DishController {
    @Autowired
    private RedisCache redisCache;

    @Autowired
    private DishService dishService;

    /**
     * 根据分类id查询菜品
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<DishVO>> list(Long categoryId) {

        //构造redis中的key，规则：dish_分类id
        String key = "dish_" + categoryId;

        //查询redis中是否存在菜品数据
        List<DishVO> list = redisCache.getCacheObject(key);
        //如果存在，直接返回，无须查询数据库
        if (list != null && !list.isEmpty()) return Result.success(list);

        //如果不存在，查询数据库，将查询到的数据放入redis中
        list = dishService.listbyCategoryId(categoryId);
        redisCache.setCacheObject(key, list);

        return Result.success(list);

    }
}