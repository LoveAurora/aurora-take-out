package org.aurora.controller.user;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.aurora.result.Result;
import org.aurora.utils.RedisCache;
import org.springframework.web.bind.annotation.*;

@RestController("userShopController")
@RequestMapping("/user/shop")
@Api(tags = "店铺相关接口")
@Slf4j
public class ShopController {

    public static final String KEY = "shop:status";

    private final RedisCache redisCache;

    public ShopController(RedisCache redisCache) {
        this.redisCache = redisCache;
    }

    /**
     * 获取店铺的营业状态
     */
    @GetMapping("/status")
    @ApiOperation("获取店铺的营业状态")
    public Result<Integer> getStatus() {
        Integer status = redisCache.getCacheObject(KEY);
        log.info("获取到店铺的营业状态为：{}", status == 1 ? "营业中" : "打烊中");
        return Result.success(status);
    }

}
