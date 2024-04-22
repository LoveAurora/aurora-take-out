package org.aurora.controller.admin;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.aurora.dto.DishDTO;
import org.aurora.dto.DishPageQueryDTO;
import org.aurora.entity.Dish;
import org.aurora.result.PageResult;
import org.aurora.result.Result;
import org.aurora.service.DishService;
import org.aurora.utils.RedisCache;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 菜品(Dish)表控制层
 *
 * @author Aurora
 * @since 2024-04-17 15:24:45
 */
@RestController
@Slf4j
@RequestMapping("/admin/dish")
public class DishController {
    private final DishService dishService;
    private final RedisCache redisCache;

    // 构造器注入
    public DishController(DishService dishService, RedisCache redisCache) {
        this.dishService = dishService;
        this.redisCache = redisCache;
    }

    /**
     * 新增菜品
     */
    @PostMapping
    @ApiOperation("新增菜品")
    public Result<String> addDish(@RequestBody DishDTO dishDTO) {
        log.info("新增菜品:{}", dishDTO);
        dishService.addDish(dishDTO);
//        redisCache.deleteObject("dish_*");
        return Result.success("新增菜品成功");
    }

    /**
     * 分页查询菜品
     */
    @GetMapping("/page")
    @ApiOperation("分页查询菜品")
    public Result<PageResult> pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        log.info("分页查询菜品:{}", dishPageQueryDTO);
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 批量删除菜品
     */
    @DeleteMapping()
    @ApiOperation("批量删除菜品")
    @CacheEvict(value = "dishCache", allEntries = true)
    public Result<String> deleteDish(@RequestParam List<Long> ids) {
        log.info("批量删除菜品:{}", ids);
        dishService.deleteDish(ids);

//        redisCache.deleteObject("dish_*");
        return Result.success("批量删除菜品成功");
    }

    /**
     * 根据id查询菜品
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询菜品")
    public Result<DishDTO> getById(@PathVariable Long id) {
        log.info("根据id查询菜品:{}", id);
        DishDTO dishDTO = dishService.getDishById(id);
        return Result.success(dishDTO);
    }

    /**
     * 修改菜品
     */
    @PutMapping
    @ApiOperation("修改菜品")
    @CacheEvict(value = "dishCache", allEntries = true)
    public Result<String> updateDish(@RequestBody DishDTO dishDTO) {
        log.info("修改菜品:{}", dishDTO);
        dishService.updateDish(dishDTO);
//        redisCache.deleteObject("dish_*");
        return Result.success();
    }

    /**
     * 根据分类id查询菜品
     */

    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
//    @Cacheable(cacheNames = "getByCategoryId", key = "#categoryId")
    public Result<List<Dish>> getByCategoryId(Long categoryId) {
        log.info("根据分类id查询菜品:{}", categoryId);
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Dish::getCategoryId, categoryId);
        List<Dish> dishList = dishService.list(queryWrapper);
        return Result.success(dishList);
    }

    /**
     * 修改菜品状态
     */
    @PostMapping("/status/{status}")
    @ApiOperation("修改菜品状态")
    @CacheEvict(value = "dishCache", allEntries = true)
    public Result<String> updateStatus(@PathVariable("status") Integer status, Long id) {
        log.info("修改菜品状态:{},{}", status, id);
        dishService.updateStatus(status, id);
//        redisCache.deleteObject("dish_*");
        return Result.success();
    }
}

