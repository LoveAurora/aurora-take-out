package org.aurora.service;

import org.aurora.vo.OrderReportVO;
import org.aurora.vo.SalesTop10ReportVO;
import org.aurora.vo.TurnoverReportVO;
import org.aurora.vo.UserReportVO;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;

public interface ReportService {
    TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end);

    UserReportVO getUserStatistics(LocalDate begin, LocalDate end);

    OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end);

    void exportBusinessData(HttpServletResponse response);

    SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end);
}
