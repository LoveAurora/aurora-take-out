package org.aurora.controller.admin;

import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.aurora.dto.SetmealDTO;
import org.aurora.dto.SetmealPageQueryDTO;
import org.aurora.result.PageResult;
import org.aurora.result.Result;
import org.aurora.service.SetmealService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 套餐(Setmeal)表控制层
 */
@RestController
@Slf4j
@RequestMapping("/admin/setmeal")
public class SetmealController {

    // 自动注入SetmealService
    private final SetmealService setmealService;

    public SetmealController(SetmealService setmealService) {
        this.setmealService = setmealService;
    }

    /**
     * 分页查询套餐
     *
     * @param setmealPageQueryDTO 分页查询参数
     * @return 分页查询结果
     */
    @GetMapping("/page")
    @ApiOperation("分页查询套餐")
//    @Cacheable(cacheNames = "pageQuery", key = "#setmealPageQueryDTO.page")
    public Result<PageResult> pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        log.info("分页查询套餐，参数：{}", setmealPageQueryDTO);
        PageResult pageResult = setmealService.pageQuery(setmealPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 修改套餐
     *
     * @param setmealDTO 套餐信息
     * @return 操作结果
     */
    @PutMapping
    @ApiOperation("修改套餐")
    @CacheEvict(cacheNames = "setmealCache", allEntries = true)
    public Result<String> updateMeal(@RequestBody SetmealDTO setmealDTO) {
        log.info("修改套餐，参数：{}", setmealDTO);
        setmealService.updateMeal(setmealDTO);
        return Result.success();
    }

    /**
     * 新增套餐
     *
     * @param setmealDTO 套餐信息
     * @return 操作结果
     */
    @PostMapping
    @ApiOperation("新增套餐")
    public Result<String> addMeal(@RequestBody SetmealDTO setmealDTO) {
        log.info("新增套餐，参数：{}", setmealDTO);
        setmealService.addMeal(setmealDTO);
        return Result.success();
    }

    /**
     * 修改套餐状态
     *
     * @param status 套餐状态
     * @param id     套餐ID
     * @return 操作结果
     */
    @PostMapping("/status/{status}")
    @ApiOperation("修改套餐状态")
    @CacheEvict(cacheNames = "setmealCache", allEntries = true)
    public Result<String> updateStatus(@PathVariable Integer status, Long id) {
        log.info("修改套餐状态:{},{}", status, id);
        setmealService.updateStatus(status, id);
        return Result.success("修改套餐状态成功");
    }

    /**
     * 批量删除套餐
     *
     * @param ids 套餐ID列表
     * @return 操作结果
     */
    @DeleteMapping
    @ApiOperation("批量删除套餐")
    @CacheEvict(cacheNames = "setmealCache",  allEntries = true)
    public Result<String> deleteMeals(@RequestParam List<Long> ids) {
        log.info("批量删除套餐，参数：{}", ids);
        setmealService.deleteMeals(ids);
        return Result.success("");
    }

    /**
     * 根据id查询套餐
     *
     * @param id 套餐ID
     * @return 查询结果
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询套餐")
    public Result<SetmealDTO> getById(@PathVariable Long id) {
        log.info("根据id查询套餐，参数：{}", id);
        SetmealDTO setmealDTO = setmealService.getDishById(id);
        return Result.success(setmealDTO);
    }
}