package org.aurora.controller.admin;


import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.aurora.dto.SetmealDTO;
import org.aurora.dto.SetmealPageQueryDTO;
import org.aurora.entity.Dish;
import org.aurora.entity.Setmeal;
import org.aurora.result.PageResult;
import org.aurora.result.Result;
import org.aurora.service.SetmealDishService;
import org.aurora.service.SetmealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 套餐(Setmeal)表控制层
 *
 * @author Aurora
 * @since 2024-04-17 15:25:23
 */
@RestController
@Slf4j
@RequestMapping("/admin/setmeal")
public class SetmealController {

    @Autowired
    private SetmealService setmealService;


    @GetMapping("/page")
    @ApiOperation("分页查询套餐")
    public Result<PageResult> pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        log.info("分页查询套餐，参数：{}", setmealPageQueryDTO);
        PageResult pageResult = setmealService.pageQuery(setmealPageQueryDTO);
        return Result.success(pageResult);
    }

    @PutMapping
    @ApiOperation("修改套餐")
    public Result<String> updateMeal(@RequestBody SetmealDTO setmealDTO) {
        log.info("修改套餐，参数：{}", setmealDTO);
        setmealService.updateMeal(setmealDTO);
        return Result.success();
    }


    @PostMapping
    @ApiOperation("新增套餐")
    public Result<String> addMeal(@RequestBody SetmealDTO setmealDTO) {
        log.info("新增套餐，参数：{}", setmealDTO);
        setmealService.addMeal(setmealDTO);
        return Result.success();
    }

    @PostMapping("/status/{status}")
    @ApiOperation("修改套餐状态")
    public Result<String> updateStatus(@PathVariable Integer status, Long id) {
        log.info("修改套餐状态:{},{}", status, id);
        boolean update = setmealService.update(new LambdaUpdateWrapper<Setmeal>()
                .eq(Setmeal::getId, id)
                .set(Setmeal::getStatus, status));
        if (!update) {
            return Result.error("修改菜品状态失败");
        }
        return Result.success();
    }

    @DeleteMapping
    @ApiOperation("批量删除套餐")
    public Result<String> deleteMeals(@RequestParam List<Long> ids) {
        log.info("批量删除套餐，参数：{}", ids);
        setmealService.deleteMeals(ids);
        setmealService.removeByIds(ids);
        return Result.success("");
    }

    @GetMapping("/{id}")
    @ApiOperation("根据id查询套餐")
    public Result<SetmealDTO> getById(@PathVariable Long id) {
        log.info("根据id查询套餐，参数：{}", id);
        SetmealDTO setmealDTO = setmealService.getDishById(id);
        return Result.success(setmealDTO);
    }

}

