package org.aurora.controller.user;


import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.aurora.result.Result;
import org.aurora.service.DishService;
import org.aurora.utils.RedisCache;
import org.aurora.vo.DishItemVO;
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
@Cacheable(cacheNames = "dishCache", key = "#categoryId")
public Result<List<DishVO>> list(Long categoryId) {
    List<DishVO> list = dishService.listbyCategoryId(categoryId);
    return Result.success(list);
}


}