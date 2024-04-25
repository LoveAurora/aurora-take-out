package org.aurora.controller.user;


import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.aurora.dto.ShoppingCartDTO;
import org.aurora.entity.ShoppingCart;
import org.aurora.result.Result;
import org.aurora.service.ShoppingCartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("userShoppingCartController")
@RequestMapping("/user/shoppingCart")
@Slf4j
@Api(tags = "C端-购物车相关接口")
public class ShoppingCartController {
    private final ShoppingCartService shoppingCartService;

    @Autowired
    public ShoppingCartController(ShoppingCartService shoppingCartService) {
        this.shoppingCartService = shoppingCartService;
    }

    @GetMapping("/list")
    public Result<List<ShoppingCart>> list() {
        log.info("查看购物车");
        List<ShoppingCart> list = shoppingCartService.shoppingCartlist();
        return Result.success(list);
    }

    @PostMapping("/add")
    public Result<String> add(@RequestBody ShoppingCartDTO shoppingCartDTO) {
        log.info("添加购物车:{}", shoppingCartDTO);
        shoppingCartService.addshoppingCart(shoppingCartDTO);
        return Result.success("添加购物车成功");
    }

    @DeleteMapping("/clean")
    public Result<String> clean() {
        log.info("清空购物车");
        shoppingCartService.cleanShopCart();
        return Result.success("清空购物车成功");
    }

    @PostMapping("/sub")
    public Result<String> sub(@RequestBody ShoppingCartDTO shoppingCartDTO) {
        log.info("购物车减少:{}", shoppingCartDTO);
        shoppingCartService.sub(shoppingCartDTO);
        return Result.success("购物车减少成功");
    }

}
