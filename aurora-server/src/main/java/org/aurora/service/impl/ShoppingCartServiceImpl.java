package org.aurora.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.aurora.context.BaseContext;
import org.aurora.dto.ShoppingCartDTO;
import org.aurora.entity.Dish;
import org.aurora.entity.Setmeal;
import org.aurora.entity.ShoppingCart;
import org.aurora.mapper.ShoppingCartMapper;
import org.aurora.service.DishService;
import org.aurora.service.ShoppingCartService;
import org.aurora.utils.BeanCopyUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 购物车(ShoppingCart)表服务实现类
 *
 * @author Aurora
 * @since 2024-04-17 15:26:14
 */
@Service("shoppingCartService")
@Slf4j
public class ShoppingCartServiceImpl extends ServiceImpl<ShoppingCartMapper, ShoppingCart> implements ShoppingCartService {
    private final DishService dishService;
    private final SetmealServiceImpl setmealService;

    public ShoppingCartServiceImpl(DishService dishService, SetmealServiceImpl setmealService) {
        this.dishService = dishService;
        this.setmealService = setmealService;
    }

    @Override
    public void addshoppingCart(ShoppingCartDTO shoppingCartDTO) {
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = BeanCopyUtils.copyBean(shoppingCartDTO, ShoppingCart.class);
        shoppingCart.setUserId(userId);
        //判断当前加入到购物车中的商品是否已经存在了 菜品或者套餐
        LambdaQueryWrapper<ShoppingCart> lambdaQuery = new LambdaQueryWrapper<>();
        lambdaQuery.eq(ShoppingCart::getUserId, userId)
                .eq(shoppingCartDTO.getDishId() != null, ShoppingCart::getDishId, shoppingCartDTO.getDishId())
                .eq(shoppingCartDTO.getSetmealId() != null, ShoppingCart::getSetmealId, shoppingCartDTO.getSetmealId());
        ShoppingCart cart = getOne(lambdaQuery);
        if (Objects.nonNull(cart)) {
            cart.setNumber(cart.getNumber() + 1);//update shopping_cart set number = ? where id = ?
            if (Objects.nonNull(shoppingCartDTO.getDishId())) {
                cart.setDishFlavor(shoppingCart.getDishFlavor());
            }
            updateById(cart);
        } else {
            //如果不存在，需要插入一条购物车数据
            //判断本次添加到购物车的是菜品还是套餐0
            Long dishId = shoppingCartDTO.getDishId();
            log.info("dishId:{}", dishId);
            if (dishId != null) {
                //本次添加到购物车的是菜品
                Dish dish = dishService.getById(dishId);
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setAmount(dish.getPrice());
            } else {
                //本次添加到购物车的是套餐
                Long setmealId = shoppingCartDTO.getSetmealId();
                log.info("dishId:{}", setmealId);
                Setmeal setmeal = setmealService.getById(setmealId);
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());
            }
            shoppingCart.setNumber(1);
            save(shoppingCart);
        }

    }

    @Override
    public void sub(ShoppingCartDTO shoppingCartDTO) {
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = BeanCopyUtils.copyBean(shoppingCartDTO, ShoppingCart.class);
        shoppingCart.setUserId(userId);

        LambdaQueryWrapper<ShoppingCart> lambdaQuery = new LambdaQueryWrapper<>();
        lambdaQuery.eq(ShoppingCart::getUserId, userId)
                .eq(shoppingCartDTO.getDishId() != null, ShoppingCart::getDishId, shoppingCartDTO.getDishId())
                .eq(shoppingCartDTO.getSetmealId() != null, ShoppingCart::getSetmealId, shoppingCartDTO.getSetmealId());

        ShoppingCart cart = getOne(lambdaQuery);

        if (Objects.nonNull(cart)) {
            int newNumber = cart.getNumber() - 1;
            if (newNumber > 0) {
                // 更新购物车中的商品数量
                cart.setNumber(newNumber);
                if (Objects.nonNull(shoppingCartDTO.getDishId())) {
                    cart.setDishFlavor(shoppingCart.getDishFlavor());
                }
                updateById(cart);
            } else if (newNumber == 0) {
                // 数量为0时，则从购物车中移除商品
                removeById(cart.getId());
            }
        }
        // 这里可能需要考虑当cart为null的情况，这意味着购物车中没有该商品。
    }

    @Override
    public List<ShoppingCart> shoppingCartlist() {
        Long userId = BaseContext.getCurrentId();
        if (userId != null) {
            LambdaQueryWrapper<ShoppingCart> lambdaQuery = new LambdaQueryWrapper<>();
            lambdaQuery.eq(ShoppingCart::getUserId, userId).orderByAsc(ShoppingCart::getCreateTime);
            List<ShoppingCart> list = list(lambdaQuery);
            return list;
        }
        return null;
    }

    @Override
    public void cleanShopCart() {
        Long userId = BaseContext.getCurrentId();
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, userId);
        remove(queryWrapper);
    }
}

