package org.aurora.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.aurora.constant.MessageConstant;
import org.aurora.context.BaseContext;
import org.aurora.dto.*;
import org.aurora.entity.*;
import org.aurora.exception.AddressBookBusinessException;
import org.aurora.exception.OrderBusinessException;
import org.aurora.exception.ShoppingCartBusinessException;
import org.aurora.mapper.OrdersMapper;
import org.aurora.result.PageResult;
import org.aurora.service.AddressBookService;
import org.aurora.service.OrdersService;
import org.aurora.service.ShoppingCartService;
import org.aurora.service.UserService;
import org.aurora.utils.BeanCopyUtils;
import org.aurora.utils.WeChatPayUtil;
import org.aurora.vo.OrderPaymentVO;
import org.aurora.vo.OrderStatisticsVO;
import org.aurora.vo.OrderSubmitVO;
import org.aurora.vo.OrderVO;
import org.aurora.websocket.WebSocketServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 订单表(Orders)表服务实现类
 *
 * @author Aurora
 * @since 2024-04-17 15:26:16
 */
@Service("ordersService")
@Slf4j
public class OrdersServiceImpl extends ServiceImpl<OrdersMapper, Orders> implements OrdersService {
    @Autowired
    private AddressBookService addressBookService;
    @Autowired
    private ShoppingCartService shoppingCartService;
    @Autowired
    private OrderDetailServiceImpl orderDetailService;
    @Autowired
    private UserService userService;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private WebSocketServer webSocketServer;
    @Autowired
    private ApplicationContext context;

    @Override
    @Transactional
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {

        //1. 处理各种业务异常（地址簿为空、购物车数据为空）
        AddressBook addressBook = addressBookService.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            //抛出业务异常
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        //检查用户的收货地址是否超出配送范围

        //查询当前用户的购物车数据
        Long userId = BaseContext.getCurrentId();
        List<ShoppingCart> shoppingCartList = shoppingCartService
                .list(new LambdaQueryWrapper<ShoppingCart>().eq(ShoppingCart::getUserId, userId));

        if (shoppingCartList == null || shoppingCartList.isEmpty()) {
            //抛出业务异常
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //2. 向订单表插入1条数据
        Orders order = BeanCopyUtils.copyBean(ordersSubmitDTO, Orders.class);
        order.setUserId(userId);
        order.setOrderTime(LocalDateTime.now());
        order.setPayStatus(Orders.UN_PAID);
        order.setStatus(Orders.PENDING_PAYMENT);
        order.setNumber(String.valueOf(System.currentTimeMillis()));
        order.setAddress(addressBook.getDetail());
        order.setConsignee(addressBook.getConsignee());
        order.setPhone(addressBook.getPhone());
        order.setUserName(addressBook.getConsignee());
        save(order);


        //3. 向订单明细表插入n条数据
        List<OrderDetail> orderDetailList = shoppingCartList.stream()
                .map(cart -> BeanCopyUtils.copyBean(cart, OrderDetail.class))
                .peek(orderDetail -> orderDetail.setOrderId(order.getId()))
                .collect(Collectors.toList());

        orderDetailService.saveBatch(orderDetailList);


        //4. 清空当前用户的购物车数据
        shoppingCartService.cleanShopCart();

        //5. 封装VO返回结果
        return OrderSubmitVO.builder()
                .id(order.getId())
                .orderTime(order.getOrderTime())
                .orderNumber(order.getNumber())
                .orderAmount(order.getAmount())
                .build();
    }

    @Value("${aurora.shop.address}")
    private String shopAddress;

    @Value("${aurora.baidu.ak}")
    private String ak;

    @Override
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userService.getById(userId);

        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
//
//        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
//            throw new OrderBusinessException("该订单已支付");
//        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", "ORDERPAID");
        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));
        //为替代微信支付成功后的数据库订单状态更新，多定义一个方法进行修改
        // 更新 订单状态，待接单  支付状态，已支付
        update(new LambdaUpdateWrapper<Orders>()
                .eq(Orders::getNumber, ordersPaymentDTO.getOrderNumber())
                .set(Orders::getPayStatus, Orders.PAID)
                .set(Orders::getStatus, Orders.TO_BE_CONFIRMED)
                .set(Orders::getCheckoutTime, LocalDateTime.now())
        );
        return vo;
    }

    @Override
    public void userCancelById(Long id) {
        update(new LambdaUpdateWrapper<Orders>()
                .eq(Objects.nonNull(id), Orders::getId, id)
                .set(Orders::getStatus, Orders.CANCELLED)
        );

    }

    @Override
    @Transactional()
    public void repetition(Long id) {
        // 再来一单 就是把上一单包含的菜品加入到购物车
        List<OrderDetail> orderDetails = orderDetailService.list(new LambdaQueryWrapper<OrderDetail>().eq(OrderDetail::getOrderId, id));
        List<OrdersRepetition> ordersRepetitions = new ArrayList<>();
        // 遍历订单明细 注意每一单的数量
        for (OrderDetail orderDetail : orderDetails) {
            ordersRepetitions.add(OrdersRepetition.builder()
                    .shoppingCartDTO(BeanCopyUtils.copyBean(orderDetail, ShoppingCartDTO.class))
                    .number(orderDetail.getNumber()).build());
        }
        for (OrdersRepetition ordersRepetition : ordersRepetitions) {
            // 获取每一类菜品的份数，调用添加到购物车的方法
            for (int i = 0; i < ordersRepetition.getNumber(); i++) {
                shoppingCartService.addshoppingCart(ordersRepetition.getShoppingCartDTO());
            }
        }
    }

    @Override
    public PageResult historyOrders(int page, int pageSize, Integer status) {
        // 获取当前用户id
        Long userId = BaseContext.getCurrentId();
        PageResult pageResult = new PageResult();
        LambdaQueryWrapper<Orders> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            // 根据状态查询订单
            lambdaQueryWrapper.eq(Orders::getUserId, userId)
                    .eq(Orders::getStatus, status)
                    .orderByDesc(Orders::getOrderTime);
        } else {
            // 根据状态查询订单
            lambdaQueryWrapper.eq(Orders::getUserId, userId)
                    .orderByDesc(Orders::getOrderTime);
        }
        Page<Orders> ordersPage = page(new Page<>(page, pageSize), lambdaQueryWrapper);
        List<OrderVO> orderVos = ordersPage.getRecords()
                .stream()
                .map(order -> {
                    OrderVO orderVO = BeanCopyUtils.copyBean(order, OrderVO.class);
                    orderVO.setOrderDetailList(orderDetailService.list(new LambdaQueryWrapper<OrderDetail>().eq(OrderDetail::getOrderId, order.getId())));
                    return orderVO;
                }).collect(Collectors.toList());

        pageResult = new PageResult(orderVos, orderVos.size());

        return pageResult;
    }

    @Override
    public OrderVO details(Long id) {
        Orders order = getById(id);
        OrderVO orderVO = BeanCopyUtils.copyBean(order, OrderVO.class);
        List<OrderDetail> list = orderDetailService.list(new LambdaQueryWrapper<OrderDetail>().eq(OrderDetail::getOrderId, id));
        orderVO.setOrderDetailList(list);
        return orderVO;
    }


    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        Page<Orders> page = new Page<>(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        LambdaQueryWrapper<Orders> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Objects.nonNull(ordersPageQueryDTO.getUserId()), Orders::getUserId, ordersPageQueryDTO.getUserId());
        lambdaQueryWrapper.eq(Objects.nonNull(ordersPageQueryDTO.getNumber()), Orders::getNumber, ordersPageQueryDTO.getNumber());
        lambdaQueryWrapper.eq(Objects.nonNull(ordersPageQueryDTO.getPhone()), Orders::getPhone, ordersPageQueryDTO.getPhone());
        lambdaQueryWrapper.eq(Objects.nonNull(ordersPageQueryDTO.getStatus()), Orders::getStatus, ordersPageQueryDTO.getStatus());
        if (Objects.nonNull(ordersPageQueryDTO.getBeginTime()) && Objects.nonNull(ordersPageQueryDTO.getEndTime())) {
            lambdaQueryWrapper.between(Orders::getOrderTime, ordersPageQueryDTO.getBeginTime(), ordersPageQueryDTO.getBeginTime());
        }
        page(page, lambdaQueryWrapper);
        return new PageResult(page.getRecords(), page.getTotal());
    }

    @Override
    public OrderStatisticsVO statistics() {
        Integer TO_BE_CONFIRMED = (int) count(new LambdaQueryWrapper<Orders>().eq(Orders::getStatus, Orders.TO_BE_CONFIRMED));
        Integer CONFIRMED = (int) count(new LambdaQueryWrapper<Orders>().eq(Orders::getStatus, Orders.CONFIRMED));
        Integer DELIVERY_IN_PROGRESS = (int) count(new LambdaQueryWrapper<Orders>().eq(Orders::getStatus, Orders.DELIVERY_IN_PROGRESS));
        return OrderStatisticsVO.builder()
                .confirmed(CONFIRMED)
                .deliveryInProgress(DELIVERY_IN_PROGRESS)
                .toBeConfirmed(TO_BE_CONFIRMED)
                .build();
    }

    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        // 根据id查询订单
        Orders ordersDB = getById(ordersRejectionDTO.getId());

        // 订单只有存在且状态为2（待接单）才可以拒单
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //支付状态
        Integer payStatus = ordersDB.getPayStatus();
        if (payStatus.equals(Orders.PAID)) {
            //用户已支付，需要退款
//                String refund = weChatPayUtil.refund(
//                        ordersDB.getNumber(),
//                        ordersDB.getNumber(),
//                        new BigDecimal(0.01),
//                        new BigDecimal(0.01));
//                log.info("申请退款：{}", refund);
            log.info("申请退款成功");

        }

        // 拒单需要退款，根据订单id更新订单状态、拒单原因、取消时间
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orders.setCancelTime(LocalDateTime.now());

        update(new LambdaUpdateWrapper<Orders>()
                .eq(Objects.nonNull(ordersRejectionDTO.getId()), Orders::getId, ordersRejectionDTO.getId())
                .set(Orders::getRejectionReason, ordersRejectionDTO.getRejectionReason())
                .set(Orders::getCancelTime, LocalDateTime.now())
                .set(Orders::getStatus, Orders.CANCELLED));
    }

    @Override
    public void delivery(Long id) {
        // 根据id查询订单
        Orders ordersDB = getById(id);
        // 校验订单是否存在，并且状态为3
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        update(new LambdaUpdateWrapper<Orders>()
                .eq(Objects.nonNull(id), Orders::getId, id)
                .set(Orders::getStatus, Orders.DELIVERY_IN_PROGRESS));
    }

    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        // 根据id查询订单
        Orders ordersDB = getById(ordersConfirmDTO.getId());
        // 校验订单是否存在，并且状态为2
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        update(new LambdaUpdateWrapper<Orders>()
                .eq(Objects.nonNull(ordersConfirmDTO.getId()), Orders::getId, ordersConfirmDTO.getId())
                .set(Orders::getStatus, Orders.CONFIRMED));
    }

    @Override
    public void complete(Long id) {
        // 根据id查询订单
        Orders ordersDB = getById(id);
        // 校验订单是否存在，并且状态为4
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        update(new LambdaUpdateWrapper<Orders>()
                .eq(Objects.nonNull(id), Orders::getId, id)
                .set(Orders::getStatus, Orders.COMPLETED));

    }

    @Override
    public void cancel(OrdersCancelDTO ordersCancelDTO) {

        // 根据id查询订单
        Orders ordersDB = getById(ordersCancelDTO.getId());

        //支付状态
        Integer payStatus = ordersDB.getPayStatus();
        if (payStatus == 1) {
            //用户已支付，需要退款
//            String refund = weChatPayUtil.refund(
//                    ordersDB.getNumber(),
//                    ordersDB.getNumber(),
//                    new BigDecimal(0.01),
//                    new BigDecimal(0.01));
//            log.info("申请退款：{}", refund);
        }

        update(new LambdaUpdateWrapper<Orders>().eq(Orders::getId, ordersCancelDTO.getId())
                .set(Orders::getStatus, Orders.CANCELLED)
                .set(Orders::getCancelReason, ordersCancelDTO.getCancelReason())
                .set(Orders::getCancelTime, LocalDateTime.now()));
    }

    public void reminder(Long id) {
        // 根据id查询订单
        Orders ordersDB = getById(id);
        // 校验订单是否存在
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Map map = new HashMap();
        map.put("type", 2); //1表示来单提醒 2表示客户催单
        map.put("orderId", id);
        map.put("content", "订单号：" + ordersDB.getNumber());

        //通过websocket向客户端浏览器推送消息
        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }

    /**
     * 根据状态和下单时间查询订单
     *
     * @param deliveryInProgress
     * @param time
     * @return
     */
    @Override
    public List<Orders> getByStatusAndOrderTimeLT(Integer deliveryInProgress, LocalDateTime time) {
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Orders::getStatus, deliveryInProgress)
                .lt(Orders::getOrderTime, time);
        return list(queryWrapper);
    }
}

