package org.aurora.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.aurora.entity.ShoppingCart;
import org.aurora.mapper.ShoppingCartMapper;
import org.aurora.service.ShoppingCartService;
import org.springframework.stereotype.Service;

/**
 * 购物车(ShoppingCart)表服务实现类
 *
 * @author Aurora
 * @since 2024-04-17 15:26:14
 */
@Service("shoppingCartService")
public class ShoppingCartServiceImpl extends ServiceImpl<ShoppingCartMapper, ShoppingCart> implements ShoppingCartService {

}

