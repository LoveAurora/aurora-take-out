package org.aurora.controller.admin;


import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.swagger.annotations.ApiOperation;
import javafx.geometry.HPos;
import lombok.extern.slf4j.Slf4j;
import org.aurora.dto.DishDTO;
import org.aurora.dto.DishPageQueryDTO;
import org.aurora.entity.Dish;
import org.aurora.result.PageResult;
import org.aurora.result.Result;
import org.aurora.service.DishService;
import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired
    private DishService dishService;

    @PostMapping
    @ApiOperation("新增菜品")
    public Result<String> addDish(@RequestBody DishDTO dishDTO) {
        log.info("新增菜品:{}", dishDTO);
        dishService.addDish(dishDTO);
        return Result.success("新增菜品成功");
    }

    @GetMapping("/page")
    @ApiOperation("分页查询菜品")
    public Result<PageResult> pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        log.info("分页查询菜品:{}", dishPageQueryDTO);
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    @DeleteMapping()
    @ApiOperation("批量删除菜品")
    public Result<String> deleteDish(@RequestParam List<Long> ids) {
        log.info("批量删除菜品:{}", ids);
        dishService.deleteDish(ids);
        return Result.success("批量删除菜品成功");
    }

    @GetMapping("/{id}")
    @ApiOperation("根据id查询菜品")
    public Result<DishDTO> getById(@PathVariable Long id) {
        log.info("根据id查询菜品:{}", id);
        DishDTO dishDTO = dishService.getDishById(id);
        return Result.success(dishDTO);
    }

    @PutMapping
    @ApiOperation("修改菜品")
    public Result<String> updateDish(@RequestBody DishDTO dishDTO) {
        log.info("修改菜品:{}", dishDTO);
        dishService.updateDish(dishDTO);
        return Result.success();
    }

    @GetMapping("")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<DishDTO>> getByCategoryId(Long categoryId) {
        log.info("根据分类id查询菜品:{}", categoryId);
        List<DishDTO> dishDTOList = dishService.getByCategoryId(categoryId);
        return Result.success(dishDTOList);
    }

    @PostMapping("/status/{status}")
    @ApiOperation("修改菜品状态")
    public Result<String> updateStatus(@PathVariable("status") Integer status, Long id) {
        log.info("修改菜品状态:{},{}", status, id);
        boolean update = dishService.update(new LambdaUpdateWrapper<Dish>()
                .eq(Dish::getId, id)
                .set(Dish::getStatus, status));
        if (!update) {
            return Result.error("修改菜品状态失败");
        }
        return Result.success();
    }
}

