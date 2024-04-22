package org.aurora.controller.user;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.aurora.dto.OrdersPaymentDTO;
import org.aurora.dto.OrdersSubmitDTO;
import org.aurora.result.PageResult;
import org.aurora.result.Result;
import org.aurora.service.OrdersService;
import org.aurora.vo.OrderPaymentVO;
import org.aurora.vo.OrderSubmitVO;
import org.aurora.vo.OrderVO;
import org.springframework.web.bind.annotation.*;

@RestController("userOrdersController")
@RequestMapping("/user/order")
@Api(tags = "用户端订单相关接口")
@Slf4j
public class OrdersController {

    private final OrdersService ordersService;

    public OrdersController(OrdersService ordersService) {
        this.ordersService = ordersService;
    }

    /**
     * 用户下单
     */
    @PostMapping("/submit")
    @ApiOperation("用户下单")
    public Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO orderSubmitDTO) {
        log.info("用户下单：{}", orderSubmitDTO);
        OrderSubmitVO orderSubmitVO = ordersService.submit(orderSubmitDTO);
        return Result.success(orderSubmitVO);
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO 订单支付状态
     */
    @PutMapping("/payment")
    @ApiOperation("订单支付")
    public Result<OrderPaymentVO> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) {
        log.info("订单支付：{}", ordersPaymentDTO);
        OrderPaymentVO orderPaymentVO = ordersService.payment(ordersPaymentDTO);
        log.info("生成预支付交易单：{}", orderPaymentVO);
        return Result.success(orderPaymentVO);
    }

    /**
     * 历史订单查询
     *
     * @param page     页码
     * @param pageSize 页大小
     * @param status   订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
     */
    @GetMapping("/historyOrders")
    @ApiOperation("历史订单查询")
    public Result<PageResult> page(int page, int pageSize, Integer status) {
        PageResult pageResult = ordersService.historyOrders(page, pageSize, status);
        return Result.success(pageResult);
    }

    /**
     * 用户取消订单
     */
    @PutMapping("/cancel/{id}")
    @ApiOperation("取消订单")
    public Result<String> cancel(@PathVariable("id") Long id) {
        log.info("订单id：{}", id);
        ordersService.userCancelById(id);
        return Result.success("取消订单成功");
    }

    /**
     * 再来一单
     */
    @PostMapping("/repetition/{id}")
    @ApiOperation("再来一单")
    public Result<String> repetition(@PathVariable("id") Long id) {
        log.info("再来一单：{}", id);
        ordersService.repetition(id);
        return Result.success();
    }


    /**
     * 查询订单详情
     */
    @GetMapping("/orderDetail/{id}")
    @ApiOperation("查询订单详情")
    public Result<OrderVO> details(@PathVariable("id") Long id) {
        log.info("查询订单详情：{}", id);
        OrderVO orderVO = ordersService.details(id);
        return Result.success(orderVO);
    }

    /**
     * 客户催单
     */
    @GetMapping("/reminder/{id}")
    @ApiOperation("客户催单")
    public Result<String> reminder(@PathVariable("id") Long id) {
        ordersService.reminder(id);
        return Result.success();
    }

}
