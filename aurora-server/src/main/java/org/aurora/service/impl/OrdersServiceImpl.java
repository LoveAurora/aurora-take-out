package org.aurora.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.aurora.entity.Orders;
import org.aurora.mapper.OrdersMapper;
import org.aurora.service.OrdersService;
import org.springframework.stereotype.Service;

/**
 * 订单表(Orders)表服务实现类
 *
 * @author Aurora
 * @since 2024-04-17 15:26:16
 */
@Service("ordersService")
public class OrdersServiceImpl extends ServiceImpl<OrdersMapper, Orders> implements OrdersService {

}

