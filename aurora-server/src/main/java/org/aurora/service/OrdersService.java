package org.aurora.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aurora.dto.*;
import org.aurora.entity.Orders;
import org.aurora.result.PageResult;
import org.aurora.vo.OrderPaymentVO;
import org.aurora.vo.OrderStatisticsVO;
import org.aurora.vo.OrderSubmitVO;
import org.aurora.vo.OrderVO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单表(Orders)表服务接口
 *
 * @author Aurora
 * @since 2024-04-17 15:26:16
 */
public interface OrdersService extends IService<Orders> {

    OrderSubmitVO submit(OrdersSubmitDTO orders);

    PageResult historyOrders(int page, int pageSize, Integer status);

    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO);

    void userCancelById(Long id);


    void repetition(Long id);

    OrderVO details(Long id);

    void cancel(OrdersCancelDTO ordersCancelDTO);

    PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);

    OrderStatisticsVO statistics();

    void rejection(OrdersRejectionDTO ordersRejectionDTO);

    void delivery(Long id);

    void confirm(OrdersConfirmDTO ordersConfirmDTO);

    void complete(Long id);

    List<Orders> getByStatusAndOrderTimeLT(Integer deliveryInProgress, LocalDateTime time);

    void reminder(Long id);
}

