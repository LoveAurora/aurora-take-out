package org.aurora.controller.admin;


import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.aurora.dto.SetmealDTO;
import org.aurora.dto.SetmealPageQueryDTO;
import org.aurora.result.PageResult;
import org.aurora.result.Result;
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
}

