package org.aurora.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.aurora.entity.OrderDetail;
import org.aurora.entity.Orders;
import org.aurora.entity.User;
import org.aurora.service.*;
import org.aurora.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service("reportService")
@Slf4j
public class ReportServiceImpl implements ReportService {
    // 注入所需的服务
    private final OrdersService ordersService;
    private final UserService userService;
    private final OrderDetailService orderDetailService;

    private final WorkspaceService workspaceService;

    @Autowired
    // 构造器注入
    public ReportServiceImpl(OrdersService ordersService, UserService userService, OrderDetailService orderDetailService, WorkspaceService workspaceService) {
        this.ordersService = ordersService;
        this.userService = userService;
        this.orderDetailService = orderDetailService;
        this.workspaceService = workspaceService;
    }

    /**
     * 获取日期列表
     */
    public List<LocalDate> dateList(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        // 前端传的是end是昨天的日期 begin是昨天的6天前的日期
        begin = begin.plusDays(0);
        end = end.plusDays(1);
        while (!begin.isAfter(end)) {
            dateList.add(begin);
            begin = begin.plusDays(1);
        }
        return dateList;
    }


    // 获取营业额统计信息
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        // 创建日期列表，包含开始日期和结束日期之间的所有日期
        List<LocalDate> dateList = dateList(begin, end);
        // 设置开始和结束时间
        LocalDateTime beginDateTime, endDateTime;
        List<BigDecimal> turnoverList = new ArrayList<>();
        // 获取在指定时间范围内的订单 每天的订单
        for (LocalDate date : dateList) {
            beginDateTime = LocalDateTime.of(date, LocalTime.MIN);
            endDateTime = LocalDateTime.of(date, LocalTime.MAX);
            List<Orders> ordersList = ordersService
                    .list(new LambdaQueryWrapper<Orders>()
                            .eq(Orders::getStatus, Orders.COMPLETED)
                            .between(Orders::getOrderTime, beginDateTime, endDateTime));
            // 如果订单列表不为空，则计算营业额
            if (ordersList != null) {
                TurnoverReportVO turnoverReportVO = new TurnoverReportVO();
                // 计算总营业额
                List<BigDecimal> amountList = ordersList.stream().map(Orders::getAmount).collect(Collectors.toList());
                BigDecimal sum = amountList.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                turnoverList.add(sum);
            } else {
                turnoverList.add(BigDecimal.ZERO);
            }
        }
        // 将日期列表、用户总数列表和新增用户数列表转换为字符串
        String dateListString = dateList.stream()
                .map(LocalDate::toString)
                .collect(Collectors.joining(", "));
        String turnoverListstring = turnoverList.stream().map(BigDecimal::toString)
                .collect(Collectors.joining(","));

        return TurnoverReportVO.builder()
                .dateList(dateListString)
                .turnoverList(turnoverListstring)
                .build();
    }

    // 获取用户统计信息
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        // 创建日期列表，包含开始日期和结束日期之间的所有日期
        List<LocalDate> dateList = dateList(begin, end);
        // 创建用户总数和新增用户数的列表
        List<String> totalUserList = new ArrayList<>();
        List<String> newUserList = new ArrayList<>();
        LocalDateTime beginDateTime, endDateTime;
        // 遍历日期列表，获取每一天的用户总数和新增用户数
        for (LocalDate date : dateList) {
            beginDateTime = LocalDateTime.of(date, LocalTime.MIN);
            endDateTime = LocalDateTime.of(date, LocalTime.MAX);

            // 获取在指定时间范围内的用户总数
            Long totalUsers = userService.count(new LambdaQueryWrapper<User>()
                    .le(User::getCreateTime, endDateTime));
            totalUserList.add(totalUsers.toString());

            // 获取在指定时间范围内的新增用户数
            Long newUsers = userService.count(new LambdaQueryWrapper<User>()
                    .between(User::getCreateTime, beginDateTime, endDateTime));
            newUserList.add(newUsers.toString());
        }

        // 将日期列表、用户总数列表和新增用户数列表转换为字符串
        String dateString = dateList.stream()
                .map(LocalDate::toString)
                .collect(Collectors.joining(", "));
        String totalUserListString = String.join(", ", totalUserList);
        String newUserListString = String.join(", ", newUserList);

        // 创建并返回用户报告对象
        return UserReportVO.builder()
                .dateList(dateString)
                .totalUserList(totalUserListString)
                .newUserList(newUserListString)
                .build();
    }

    // 获取订单统计信息
    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        // 创建日期列表，包含开始日期和结束日期之间的所有日期
        List<LocalDate> dateList = dateList(begin, end);

        // 创建订单总数和有效订单数的列表
        List<String> orderCountList = new ArrayList<>();
        List<String> validOrderCountList = new ArrayList<>();
        Long count = 0L;
        Long validCount = 0L;
        LocalDateTime beginDateTime, endDateTime;
        // 遍历日期列表，获取每一天的订单总数和有效订单数
        for (LocalDate date : dateList) {
            beginDateTime = LocalDateTime.of(date, LocalTime.MIN);
            endDateTime = LocalDateTime.of(date, LocalTime.MAX);

            count = ordersService.count(new LambdaQueryWrapper<Orders>()
                    .between(Orders::getOrderTime, beginDateTime, endDateTime));
            validCount = ordersService.count(new LambdaQueryWrapper<Orders>()
                    .eq(Orders::getStatus, Orders.COMPLETED)
                    .between(Orders::getOrderTime, beginDateTime, endDateTime));

            orderCountList.add(count.toString());
            validOrderCountList.add(validCount.toString());
        }
        // 计算订单总数和有效订单数的总和
        Integer totalOrderCount = orderCountList.stream().mapToInt(Integer::parseInt).sum();
        Integer totalValidOrderCount = validOrderCountList.stream().mapToInt(Integer::parseInt).sum();

        // 将日期列表、订单总数列表和有效订单数列表转换为字符串
        String dateString = dateList.stream()
                .map(LocalDate::toString)
                .collect(Collectors.joining(", "));
        String orderCountListString = String.join(", ", orderCountList);
        String validOrderCountListString = String.join(", ", validOrderCountList);

        // 创建并返回订单报告对象
        return OrderReportVO.builder()
                .dateList(dateString)
                .orderCountList(orderCountListString)
                .validOrderCountList(validOrderCountListString)
                .totalOrderCount(totalOrderCount)
                .validOrderCount(totalValidOrderCount)
                .orderCompletionRate((double) totalValidOrderCount / (double) totalOrderCount)
                .build();

    }

    // 导出业务数据
    @Override
    public void exportBusinessData(HttpServletResponse response) {
        //1. 查询数据库，获取营业数据---查询最近30天的运营数据
        LocalDate dateBegin = LocalDate.now().minusDays(30);
        LocalDate dateEnd = LocalDate.now().minusDays(1);

        //查询概览数据
        BusinessDataVO businessDataVO = workspaceService.getBusinessData(LocalDateTime.of(dateBegin, LocalTime.MIN), LocalDateTime.of(dateEnd, LocalTime.MAX));

        //2. 通过POI将数据写入到Excel文件中
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");

        try {
            //基于模板文件创建一个新的Excel文件
            XSSFWorkbook excel = new XSSFWorkbook(inputStream);

            //获取表格文件的Sheet页
            XSSFSheet sheet = excel.getSheet("Sheet1");

            //填充数据--时间
            sheet.getRow(1).getCell(1).setCellValue("时间：" + dateBegin + "至" + dateEnd);

            //获得第4行
            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessDataVO.getTurnover());
            row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessDataVO.getNewUsers());

            //获得第5行
            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            row.getCell(4).setCellValue(businessDataVO.getUnitPrice());

            //填充明细数据
            for (int i = 0; i < 30; i++) {
                LocalDate date = dateBegin.plusDays(i);
                //查询某一天的营业数据
                BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));

                //获得某一行
                row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());
            }

            //3. 通过输出流将Excel文件下载到客户端浏览器
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);

            //关闭资源
            out.close();
            excel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 获取销售前10的商品
    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        log.info("时间日期{}{}", begin, end);
        // 设置开始和结束时间
        // 为什么要这么处理，因为如果是某一天，前端传的end = begin
        // 如果是时间最近七天 那么end - begin = 6 所以要特殊处理
        LocalDateTime beginTime, endTime;
        if (begin.isEqual(end)) {
            beginTime = LocalDateTime.of(begin, LocalTime.MIN);
            endTime = LocalDateTime.of(end, LocalTime.MAX);
        } else {
            beginTime = LocalDateTime.of(begin, LocalTime.MIN);
            endTime = LocalDateTime.of(end.plusDays(1), LocalTime.MAX);
        }


        // 获取在指定时间范围内的订单
        List<Orders> ordersList = ordersService
                .list(new LambdaQueryWrapper<Orders>()
                        .eq(Orders::getStatus, Orders.COMPLETED)
                        .between(Orders::getOrderTime, beginTime, endTime));
        // 获取订单详情
        List<Long> ids = ordersList.stream().map(Orders::getId).collect(Collectors.toList());
        List<OrderDetail> orderDetails = orderDetailService.list(new LambdaQueryWrapper<OrderDetail>().in(OrderDetail::getOrderId, ids));
        // 如果订单详情不为空，则计算销售前10的商品
        if (!orderDetails.isEmpty()) {
            Map<String, Integer> map = new HashMap<>();
            for (OrderDetail orderDetail : orderDetails) {
                if (map.containsKey(orderDetail.getName())) {
                    int oldValue = map.get(orderDetail.getName());
                    map.put(orderDetail.getName(), oldValue + orderDetail.getNumber());
                } else {
                    map.put(orderDetail.getName(), orderDetail.getNumber());
                }
            }
            // 获取销售前10的商品名称和销售数量
            // 用entrySet()来排序
            List<String> top10Names = map.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(10)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            List<Integer> top10Value = map.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(10)
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toList());
            // 将商品名称和销售数量转换为字符串
            String namesString = String.join(", ", top10Names);
            String valuesString = top10Value.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            // 创建并返回销售前10报告对象
            return new SalesTop10ReportVO(namesString, valuesString);
        }
        return null;
    }
}