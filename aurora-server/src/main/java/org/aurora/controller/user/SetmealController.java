package org.aurora.controller.user;

import io.swagger.annotations.Api;
import org.aurora.entity.Setmeal;
import org.aurora.result.Result;
import org.aurora.service.SetmealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("userSetmealController")
@RequestMapping("/user/setmeal")
@Api(tags = "C端-套餐浏览接口")
public class SetmealController {
    @Autowired
    private SetmealService setmealService;
    @GetMapping("/list")
    public Result<List<Setmeal>> getSetMealList(Long categoryId) {
        List<Setmeal> setmealList = setmealService.getSetMealList(categoryId);
        return Result.success(setmealList);
    }
}