package org.aurora.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.aurora.entity.Dish;
import org.aurora.entity.Orders;
import org.aurora.entity.Setmeal;
import org.aurora.entity.User;
import org.aurora.service.*;
import org.aurora.vo.BusinessDataVO;
import org.aurora.vo.DishOverViewVO;
import org.aurora.vo.OrderOverViewVO;
import org.aurora.vo.SetmealOverViewVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service("workspaceService")
@Slf4j
public class WorkspaceServiceImpl implements WorkspaceService {

    private final OrdersService ordersService;
    private final UserService userService;
    private final DishService dishService;
    private final SetmealService setmealService;

    public WorkspaceServiceImpl(OrdersService ordersService, UserService userService, DishService dishService, SetmealService setmealService) {
        this.ordersService = ordersService;
        this.userService = userService;
        this.dishService = dishService;
        this.setmealService = setmealService;
    }


    /**
     * 根据时间段统计营业数据
     *
     * @param begin 开始时间
     * @param end   结束时间
     * @return 返回营业额
     */
    public BusinessDataVO getBusinessData(LocalDateTime begin, LocalDateTime end) {
        List<Orders> ordersList = ordersService.list(new LambdaQueryWrapper<Orders>()
                .between(Orders::getOrderTime, begin, end)
                .eq(Orders::getStatus, Orders.COMPLETED));

        double turnover = ordersList.stream()
                .map(Orders::getAmount)
                .mapToDouble(BigDecimal::doubleValue)
                .sum();

        int validOrderCount = ordersList.size();
        List<Orders> allOrdersList = ordersService.list(new LambdaQueryWrapper<Orders>()
                .between(Orders::getOrderTime, begin, end));

        double orderCompletionRate;
        if (validOrderCount != 0) {
            orderCompletionRate = validOrderCount / (double) allOrdersList.size();
        } else {
            orderCompletionRate = 0.0;
        }
        double unitPrice = 0;
        if (!ordersList.isEmpty())
            unitPrice = turnover / (double) ordersList.stream().map(Orders::getUserId).distinct().count();


        Integer newUsers = userService.list(new LambdaQueryWrapper<User>()
                .between(User::getCreateTime, begin, end)).size();

        return BusinessDataVO.builder()
                .turnover(turnover)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .unitPrice(unitPrice)
                .newUsers(newUsers)
                .build();

    }


    /**
     * 查询订单管理数据
     */
    public OrderOverViewVO getOrderOverView() {
        List<Orders> ordersList = ordersService.list();
        if (!ordersList.isEmpty()) {
            int allOrders = ordersList.size();
            int completedOrders = (int) ordersList.stream().filter(orders -> orders.getStatus().equals(Orders.COMPLETED)).count();
            int cancelledOrders = (int) ordersList.stream().filter(orders -> orders.getStatus().equals(Orders.CANCELLED)).count();
            int deliveredOrders = (int) ordersList.stream().filter(orders -> orders.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)).count();
            int waitingOrders = (int) ordersList.stream().filter(orders -> orders.getStatus().equals(Orders.TO_BE_CONFIRMED)).count();
            return OrderOverViewVO.builder()
                    .completedOrders(completedOrders)
                    .cancelledOrders(cancelledOrders)
                    .allOrders(allOrders)
                    .waitingOrders(waitingOrders)
                    .deliveredOrders(deliveredOrders)
                    .build();
        }
        return OrderOverViewVO.builder()
                .completedOrders(0)
                .cancelledOrders(0)
                .allOrders(0)
                .waitingOrders(0)
                .deliveredOrders(0)
                .build();
    }

    /**
     * 查询菜品总览
     */
    public DishOverViewVO getDishOverView() {

        List<Dish> dishes = dishService.list();
        if (!dishes.isEmpty()) {
            int sold = (int) dishes.stream().filter(dish -> dish.getStatus().equals(1)).count();
            int discontinued = dishes.size() - sold;
            return DishOverViewVO.builder()
                    .sold(sold)
                    .discontinued(discontinued)
                    .build();
        }
        return DishOverViewVO.builder()
                .sold(0)
                .discontinued(0)
                .build();
    }

    /**
     * 查询套餐总览
     */
    public SetmealOverViewVO getSetmealOverView() {
        List<Setmeal> setmeals = setmealService.list();
        if (!setmeals.isEmpty()) {
            int sold = (int) setmeals.stream().map(dish -> dish.getStatus() == 1).count();
            int discontinued = setmeals.size() - sold;
            return SetmealOverViewVO.builder()
                    .sold(sold)
                    .discontinued(discontinued)
                    .build();
        }
        return SetmealOverViewVO.builder()
                .sold(0)
                .discontinued(0)
                .build();
    }
}
