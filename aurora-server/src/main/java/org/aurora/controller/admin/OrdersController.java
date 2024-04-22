package org.aurora.controller.admin;


import io.swagger.annotations.ApiOperation;
import org.aurora.dto.OrdersCancelDTO;
import org.aurora.dto.OrdersConfirmDTO;
import org.aurora.dto.OrdersPageQueryDTO;
import org.aurora.dto.OrdersRejectionDTO;
import org.aurora.result.PageResult;
import org.aurora.result.Result;
import org.aurora.service.OrdersService;
import org.aurora.vo.OrderStatisticsVO;
import org.aurora.vo.OrderVO;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;


/**
 * 订单表(Orders)表控制层
 *
 * @author Aurora
 * @since 2024-04-17 15:26:16
 */
@RestController
@RequestMapping("/admin/order/")
public class OrdersController {
    private final OrdersService ordersService;

    // 构造器注入
    public OrdersController(OrdersService ordersService) {
        this.ordersService = ordersService;
    }

    /**
     * 订单搜索
     */
    @GetMapping("/conditionSearch")
    @ApiOperation("订单搜索")
    public Result<PageResult> conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageResult pageResult = ordersService.conditionSearch(ordersPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 各个状态的订单数量统计
     */
    @GetMapping("/statistics")
    @ApiOperation("各个状态的订单数量统计")
    public Result<OrderStatisticsVO> statistics() {
        OrderStatisticsVO orderStatisticsVO = ordersService.statistics();
        return Result.success(orderStatisticsVO);
    }

    /**
     * 订单详情
     */
    @GetMapping("/details/{id}")
    @Cacheable(cacheNames = "orderDetails", key = "#id")
    public Result<OrderVO> details(@PathVariable("id") Long id) {
        OrderVO orderVO = ordersService.details(id);
        return Result.success(orderVO);
    }

    /**
     * 接单
     */
    @PutMapping("/confirm")
    @ApiOperation("接单")
    public Result<String> confirm(@RequestBody OrdersConfirmDTO ordersConfirmDTO) {
        ordersService.confirm(ordersConfirmDTO);
        return Result.success();
    }

    /**
     * 拒单
     */
    @PutMapping("/rejection")
    @ApiOperation("拒单")
    public Result<String> rejection(@RequestBody OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        ordersService.rejection(ordersRejectionDTO);
        return Result.success();
    }

    /**
     * 取消订单
     */
    @PutMapping("/cancel")
    @ApiOperation("取消订单")
    public Result<String> cancel(@RequestBody OrdersCancelDTO ordersCancelDTO) throws Exception {
        ordersService.cancel(ordersCancelDTO);
        return Result.success();
    }

    /**
     * 派送订单
     */
    @PutMapping("/delivery/{id}")
    @ApiOperation("派送订单")
    public Result<String> delivery(@PathVariable("id") Long id) {
        ordersService.delivery(id);
        return Result.success();
    }

    /**
     * 完成订单
     */
    @PutMapping("/complete/{id}")
    @ApiOperation("完成订单")
    public Result<String> complete(@PathVariable("id") Long id) {
        ordersService.complete(id);
        return Result.success();
    }
}

