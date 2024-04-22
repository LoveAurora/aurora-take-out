package org.aurora.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
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
import org.aurora.utils.GetDistance;
import org.aurora.utils.HttpClientUtil;
import org.aurora.utils.WeChatPayUtil;
import org.aurora.vo.OrderPaymentVO;
import org.aurora.vo.OrderStatisticsVO;
import org.aurora.vo.OrderSubmitVO;
import org.aurora.vo.OrderVO;
import org.aurora.websocket.WebSocketServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
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
    private final AddressBookService addressBookService;
    private final ShoppingCartService shoppingCartService;
    private final OrderDetailServiceImpl orderDetailService;
    private final UserService userService;
    private final WeChatPayUtil weChatPayUtil;
    private final WebSocketServer webSocketServer;
    private final ApplicationContext context;

    // 构造器注入
    public OrdersServiceImpl(AddressBookService addressBookService, ShoppingCartService shoppingCartService, OrderDetailServiceImpl orderDetailService, UserService userService, WeChatPayUtil weChatPayUtil, WebSocketServer webSocketServer, ApplicationContext context) {
        this.addressBookService = addressBookService;
        this.shoppingCartService = shoppingCartService;
        this.orderDetailService = orderDetailService;
        this.userService = userService;
        this.weChatPayUtil = weChatPayUtil;
        this.webSocketServer = webSocketServer;
        this.context = context;
    }

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
        checkOutOfRange(addressBook.getCityName() + addressBook.getDistrictName() + addressBook.getDetail());
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


    @Override
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userService.getById(userId);

        // 调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = null;
//        try {
//            jsonObject = weChatPayUtil.pay(
//                    ordersPaymentDTO.getOrderNumber(), //商户订单号
//                    new BigDecimal("0.01"), //支付金额，单位 元
//                    "苍穹外卖订单", //商品描述
//                    user.getOpenid() //微信用户的openid
//            );
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }

//        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
//            throw new OrderBusinessException("该订单已支付");
//        }
        // 跳过支付接口的做法
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

    /**
     * 支付成功，修改订单状态
     */
    @Override
    public void paySuccess(String outTradeNo) {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();

        // 根据订单号查询当前用户的订单
//        Orders ordersDB = getByNumberAndUserId(outTradeNo, userId);
        Orders ordersDB = getOne(new LambdaUpdateWrapper<Orders>()
                .eq(Orders::getNumber, outTradeNo)
                .eq(Orders::getUserId, userId));

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        updateById(orders);

        //通过websocket向客户端浏览器推送消息 type orderId content
        Map<String, Object> map = new HashMap<>();
        map.put("type", 1); // 1表示来单提醒 2表示客户催单
        map.put("orderId", ordersDB.getId());
        map.put("content", "订单号：" + outTradeNo);

        String json = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json);
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
        // 创建一个新的分页结果对象
        PageResult pageResult = new PageResult();
        // 创建查询条件
        LambdaQueryWrapper<Orders> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            // 如果状态不为空，根据用户id和状态查询订单，并按订单时间降序排序
            lambdaQueryWrapper.eq(Orders::getUserId, userId)
                    .eq(Orders::getStatus, status)
                    .orderByDesc(Orders::getOrderTime);
        } else {
            // 如果状态为空，根据用户id查询订单，并按订单时间降序排序
            lambdaQueryWrapper.eq(Orders::getUserId, userId)
                    .orderByDesc(Orders::getOrderTime);
        }
        // 执行分页查询
        Page<Orders> ordersPage = page(new Page<>(page, pageSize), lambdaQueryWrapper);
        // 将查询结果转换为OrderVO对象，并获取每个订单的详细信息
        List<OrderVO> orderVos = ordersPage.getRecords()
                .stream()
                .map(order -> {
                    OrderVO orderVO = BeanCopyUtils.copyBean(order, OrderVO.class);
                    orderVO.setOrderDetailList(orderDetailService.list(new LambdaQueryWrapper<OrderDetail>().eq(OrderDetail::getOrderId, order.getId())));
                    return orderVO;
                }).collect(Collectors.toList());

        // 设置分页结果的记录和总数
        pageResult = new PageResult(orderVos, orderVos.size());

        // 返回分页结果
        return pageResult;
    }

    @Override

    public OrderVO details(Long id) {
        // 根据ID获取订单
        Orders order = getById(id);
        // 将订单实体对象转换为视图对象
        OrderVO orderVO = BeanCopyUtils.copyBean(order, OrderVO.class);
        // 获取订单的所有明细
        List<OrderDetail> list = orderDetailService.list(new LambdaQueryWrapper<OrderDetail>().eq(OrderDetail::getOrderId, id));
        // 将订单明细设置到视图对象中
        orderVO.setOrderDetailList(list);
        // 返回包含订单详情的视图对象
        return orderVO;
    }

    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        // 创建分页对象
        Page<Orders> page = new Page<>(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        // 创建查询条件
        LambdaQueryWrapper<Orders> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        // 如果用户ID不为空，添加用户ID查询条件
        lambdaQueryWrapper.eq(Objects.nonNull(ordersPageQueryDTO.getUserId()), Orders::getUserId, ordersPageQueryDTO.getUserId());
        // 如果订单号不为空，添加订单号查询条件
        lambdaQueryWrapper.eq(Objects.nonNull(ordersPageQueryDTO.getNumber()), Orders::getNumber, ordersPageQueryDTO.getNumber());
        // 如果电话号码不为空，添加电话号码查询条件
        lambdaQueryWrapper.eq(Objects.nonNull(ordersPageQueryDTO.getPhone()), Orders::getPhone, ordersPageQueryDTO.getPhone());
        // 如果状态不为空，添加状态查询条件
        lambdaQueryWrapper.eq(Objects.nonNull(ordersPageQueryDTO.getStatus()), Orders::getStatus, ordersPageQueryDTO.getStatus());
        // 如果开始时间和结束时间都不为空，添加订单时间查询条件
        if (Objects.nonNull(ordersPageQueryDTO.getBeginTime()) && Objects.nonNull(ordersPageQueryDTO.getEndTime())) {
            lambdaQueryWrapper.between(Orders::getOrderTime, ordersPageQueryDTO.getBeginTime(), ordersPageQueryDTO.getBeginTime());
        }
        // 执行分页查询
        page(page, lambdaQueryWrapper);
        // 返回分页结果
        return new PageResult(page.getRecords(), page.getTotal());
    }

    @Override
    public OrderStatisticsVO statistics() {
        // 查询待确认的订单数量
        Integer TO_BE_CONFIRMED = (int) count(new LambdaQueryWrapper<Orders>().eq(Orders::getStatus, Orders.TO_BE_CONFIRMED));
        // 查询已确认的订单数量
        Integer CONFIRMED = (int) count(new LambdaQueryWrapper<Orders>().eq(Orders::getStatus, Orders.CONFIRMED));
        // 查询正在配送的订单数量
        Integer DELIVERY_IN_PROGRESS = (int) count(new LambdaQueryWrapper<Orders>().eq(Orders::getStatus, Orders.DELIVERY_IN_PROGRESS));
        // 构建并返回订单统计对象
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

    /**
     * 检查客户的收货地址是否超出配送范围
     */
    @Value("${aurora.shop.address}")
    private String shopAddress;

    private void checkOutOfRange(String address) {

        String getDistance = GetDistance.checkOutOfRange(shopAddress, address);
        int distance = Integer.parseInt(getDistance);
        if (distance > 5000) {
            //配送距离超过5000米
            throw new OrderBusinessException("超出配送范围");
        }
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
        // if (payStatus == 1) {
        //用户已支付，需要退款
//            String refund = weChatPayUtil.refund(
//                    ordersDB.getNumber(),
//                    ordersDB.getNumber(),
//                    new BigDecimal(0.01),
//                    new BigDecimal(0.01));
//            log.info("申请退款：{}", refund);
        //   }

        update(new LambdaUpdateWrapper<Orders>().eq(Orders::getId, ordersCancelDTO.getId())
                .set(Orders::getStatus, Orders.CANCELLED)
                .set(Orders::getCancelReason, ordersCancelDTO.getCancelReason())
                .set(Orders::getCancelTime, LocalDateTime.now()));
    }


    public void reminder(Long id) {
        // 根据id查询订
        Orders ordersDB = getById(id);
        // 校验订单是否存在
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Map<String, Object> map = new HashMap<>();
        map.put("type", 2); //1表示来单提醒 2表示客户催单
        map.put("orderId", id);
        map.put("content", "订单号：" + ordersDB.getNumber());

        //通过websocket向客户端浏览器推送消息
        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }


    @Override
    public List<Orders> getByStatusAndOrderTimeLT(Integer deliveryInProgress, LocalDateTime time) {
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Orders::getStatus, deliveryInProgress)
                .lt(Orders::getOrderTime, time);
        return list(queryWrapper);
    }
}

