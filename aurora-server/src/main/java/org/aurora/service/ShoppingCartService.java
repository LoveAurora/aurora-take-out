package org.aurora.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aurora.dto.ShoppingCartDTO;
import org.aurora.entity.ShoppingCart;

import java.util.List;

/**
 * 购物车(ShoppingCart)表服务接口
 *
 * @author Aurora
 * @since 2024-04-17 15:26:14
 */
public interface ShoppingCartService extends IService<ShoppingCart> {

    void addshoppingCart(ShoppingCartDTO shoppingCartDTO);

    List<ShoppingCart> shoppingCartlist();

    void sub(ShoppingCartDTO shoppingCartDTO);
}

