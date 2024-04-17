package org.aurora.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.aurora.entity.Employee;

/**
 * 员工信息(Employee)表数据库访问层
 *
 * @author Aurora
 * @since 2024-04-17 17:37:51
 */
@Mapper
public interface EmployeeMapper extends BaseMapper<Employee> {
    @Select("select * from employee where username = #{username}")
    Employee getByUsername(String username);
}
