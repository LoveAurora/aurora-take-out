package org.aurora.controller.user;


import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.aurora.result.Result;
import org.aurora.service.DishService;
import org.aurora.utils.RedisCache;
import org.aurora.vo.DishVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
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
@ApiOperation("C端-菜品接口")
public class DishController {
    private final DishService dishService;
    private final RedisCache redisCache;

    public DishController(DishService dishService, RedisCache redisCache) {
        this.dishService = dishService;
        this.redisCache = redisCache;
    }

    /**
     * 根据分类id查询菜品
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    @Cacheable(cacheNames = "dishCache", key = "#categoryId") // 采用spring框架集成的redis缓存
    public Result<List<DishVO>> list(Long categoryId) {
        log.info("根据分类id查询菜品{}", categoryId);
        //构造redis中的key，规则：dish_分类id
//        String key = "dish:" + categoryId;
        //查询redis中是否存在菜品数据
//        List<DishVO> list = redisCache.getCacheList(key);
//        if (list != null && !list.isEmpty()) {
//            //如果存在，直接返回，无须查询数据库
//            return Result.success(list);
//        }
        List<DishVO> listDishVO = dishService.listbyCategoryId(categoryId);
        //如果不存在，查询数据库，将查询到的数据放入redis中
//        redisCache.setCacheObject(key, listDishVO);
        return Result.success(listDishVO);
    }


}