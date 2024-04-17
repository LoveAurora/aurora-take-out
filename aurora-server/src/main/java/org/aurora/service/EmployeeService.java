package org.aurora.service;


import org.aurora.dto.EmployeeLoginDTO;
import org.aurora.entity.Employee;

public interface EmployeeService {

    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    Employee login(EmployeeLoginDTO employeeLoginDTO);

}
