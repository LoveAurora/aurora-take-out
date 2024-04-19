package org.aurora.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.aurora.constant.MessageConstant;
import org.aurora.constant.StatusConstant;
import org.aurora.dto.EmployeeLoginDTO;
import org.aurora.dto.EmployeePageQueryDTO;
import org.aurora.dto.PasswordEditDTO;
import org.aurora.entity.Employee;
import org.aurora.exception.AccountLockedException;
import org.aurora.exception.AccountNotFoundException;
import org.aurora.exception.PasswordErrorException;
import org.aurora.mapper.EmployeeMapper;
import org.aurora.result.PageResult;
import org.aurora.service.EmployeeService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 员工信息(Employee)表服务实现类
 *
 * @author Aurora
 * @since 2024-04-17 15:26:04
 */
@Service("employeeService")
public class EmployeeServiceImpl extends ServiceImpl<EmployeeMapper, Employee> implements EmployeeService {



    /**
     * 员工登录
     *
     * @param employeeLoginDTO 登录信息
     * @return 登录成功的员工信息
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        // 根据用户名查询数据库中的数据
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Employee::getUsername, username);
        Employee employee = getOne(queryWrapper);

        // 处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            // 账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        // 密码比对
        // TODO 后期需要进行md5加密，然后再进行比对
        if (!password.equals(employee.getPassword())) {
            // 密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (employee.getStatus().equals(StatusConstant.DISABLE)) {
            // 账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        // 返回实体对象
        return employee;
    }

    /**
     * 修改密码
     *
     * @param passwordEditDTO 密码修改信息
     */
    @Override
    public void editPassword(PasswordEditDTO passwordEditDTO) {
        // 根据员工id查询员工信息
        Employee employee = getById(passwordEditDTO.getEmpId());
        // 比对旧密码是否正确
        if (!employee.getPassword().equals(passwordEditDTO.getOldPassword())) {
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }
        // 更新密码
        employee.setPassword(passwordEditDTO.getNewPassword());
        updateById(employee);
    }

    /**
     * 修改状态
     *
     * @param status 新的状态
     */
    @Override
    public void updateStatus(Integer status) {
        // 根据员工id查询员工信息
        Employee employee = getById(status);
        // 更新状态
        employee.setStatus(status);
        updateById(employee);
    }

    /**
     * 分页查询
     *
     * @param employeePageQueryDTO 分页查询信息
     * @return 分页结果
     */
    @Override
    public PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO) {
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StringUtils.hasText(employeePageQueryDTO.getName()), Employee::getName, employeePageQueryDTO.getName());
        Page<Employee> page = new Page<>();
        page.setSize(employeePageQueryDTO.getPageSize());
        page.setCurrent(employeePageQueryDTO.getPage());
        page(page, queryWrapper);
        return new PageResult(page.getRecords(), page.getTotal());
    }
}