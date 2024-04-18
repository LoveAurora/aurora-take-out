package org.aurora.service.impl;


import lombok.extern.slf4j.Slf4j;
import org.aurora.constant.StatusConstant;
import org.aurora.entity.Orders;
import org.aurora.mapper.DishMapper;
import org.aurora.mapper.OrdersMapper;
import org.aurora.mapper.SetmealMapper;
import org.aurora.mapper.UserMapper;
import org.aurora.service.WorkspaceService;
import org.aurora.vo.BusinessDataVO;
import org.aurora.vo.DishOverViewVO;
import org.aurora.vo.OrderOverViewVO;
import org.aurora.vo.SetmealOverViewVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class WorkspaceServiceImpl implements WorkspaceService {

    @Autowired
    private OrdersMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 根据时间段统计营业数据
     * @param begin
     * @param end
     * @return
     */
    public BusinessDataVO getBusinessData(LocalDateTime begin, LocalDateTime end) {
    return null;

    }


    /**
     * 查询订单管理数据
     *
     * @return
     */
    public OrderOverViewVO getOrderOverView() {
        return null;
    }

    /**
     * 查询菜品总览
     *
     * @return
     */
    public DishOverViewVO getDishOverView() {
        return null;
    }

    /**
     * 查询套餐总览
     *
     * @return
     */
    public SetmealOverViewVO getSetmealOverView() {
        return null;
    }
}
