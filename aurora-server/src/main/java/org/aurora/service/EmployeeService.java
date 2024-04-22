package org.aurora.service;


import com.baomidou.mybatisplus.extension.service.IService;
import org.aurora.dto.EmployeeLoginDTO;
import org.aurora.dto.EmployeePageQueryDTO;
import org.aurora.dto.PasswordEditDTO;
import org.aurora.entity.Employee;
import org.aurora.result.PageResult;

public interface EmployeeService extends IService<Employee>{


    Employee login(EmployeeLoginDTO employeeLoginDTO);

    void editPassword(PasswordEditDTO employeeLoginDTO);

    void updateStatus(Integer status);

    PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO);
}
