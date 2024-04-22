package org.aurora.service;

import org.aurora.vo.BusinessDataVO;
import org.aurora.vo.DishOverViewVO;
import org.aurora.vo.OrderOverViewVO;
import org.aurora.vo.SetmealOverViewVO;

import java.time.LocalDateTime;

public interface WorkspaceService {


    BusinessDataVO getBusinessData(LocalDateTime begin, LocalDateTime end);


    OrderOverViewVO getOrderOverView();

    DishOverViewVO getDishOverView();

    SetmealOverViewVO getSetmealOverView();

}
