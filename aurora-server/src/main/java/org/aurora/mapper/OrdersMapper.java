package org.aurora.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;
import org.aurora.entity.Orders;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单表(Orders)表数据库访问层
 *
 * @author Aurora
 * @since 2024-04-17 17:37:51
 */
public interface OrdersMapper extends BaseMapper<Orders> {

}
