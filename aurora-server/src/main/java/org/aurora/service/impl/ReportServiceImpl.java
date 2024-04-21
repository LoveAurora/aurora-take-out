package org.aurora.service.impl;

import org.aurora.service.ReportService;
import org.aurora.vo.OrderReportVO;
import org.aurora.vo.SalesTop10ReportVO;
import org.aurora.vo.TurnoverReportVO;
import org.aurora.vo.UserReportVO;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;


@Service("reportService")
public class ReportServiceImpl implements ReportService {
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {

        return null;
    }

    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        return null;
    }

    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        return null;
    }

    @Override
    public void exportBusinessData(HttpServletResponse response) {

    }

    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        return null;
    }

    public ReportServiceImpl() {
        super();
    }
}
