package org.aurora.controller.admin;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.aurora.constant.JwtClaimsConstant;
import org.aurora.constant.PasswordConstant;
import org.aurora.dto.EmployeeDTO;
import org.aurora.dto.EmployeeLoginDTO;
import org.aurora.dto.EmployeePageQueryDTO;
import org.aurora.dto.PasswordEditDTO;
import org.aurora.entity.Employee;
import org.aurora.properties.JwtProperties;
import org.aurora.result.PageResult;
import org.aurora.result.Result;
import org.aurora.service.EmployeeService;
import org.aurora.utils.BeanCopyUtils;
import org.aurora.utils.JwtUtil;
import org.aurora.vo.EmployeeLoginVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 员工管理
 */
@RestController
@RequestMapping("/admin/employee")
@Slf4j
@Api(tags = "员工管理相关接口")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final JwtProperties jwtProperties;

    // 构造器注入
    public EmployeeController(JwtProperties jwtProperties, EmployeeService employeeService) {
        this.jwtProperties = jwtProperties;
        this.employeeService = employeeService;
    }


    /**
     * 登录
     */
    @PostMapping("/login")
    @ApiOperation("员工登录")
    public Result<EmployeeLoginVO> login(@RequestBody EmployeeLoginDTO employeeLoginDTO) {
        log.info("员工登录：{}", employeeLoginDTO);
        Employee employee = employeeService.login(employeeLoginDTO);
        //登录成功后，生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, employee.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims);

        EmployeeLoginVO employeeLoginVO = EmployeeLoginVO.builder()
                .id(employee.getId())
                .userName(employee.getUsername())
                .name(employee.getName())
                .token(token)
                .build();

        return Result.success(employeeLoginVO);
    }

    /**
     * 退出
     */
    @PostMapping("/logout")
    public Result<String> logout() {
        return Result.success();
    }

    /**
     * 修改密码
     */
    @PutMapping("/editPassword")
    @ApiOperation("修改密码")
    public Result<String> editPassword(@RequestBody PasswordEditDTO passwordEditDTO) {
        log.info("修改密码：{}", passwordEditDTO);
        employeeService.editPassword(passwordEditDTO);
        return Result.success();
    }

    /**
     * 修改状态
     */
    @PostMapping("/status/{status}")
    @ApiOperation("修改状态")
    public Result<String> updateStatus(@PathVariable("status") Integer status) {
        log.info("修改状态：{}", status);
        employeeService.updateStatus(status);
        return Result.success();
    }

    /**
     * 分页查询
     */
    @GetMapping("/page")
    @ApiOperation("分页查询")
    public Result<PageResult> pageQuery(EmployeePageQueryDTO employeePageQueryDTO) {
        log.info("分页查询：{}", employeePageQueryDTO);
        PageResult pageResult = employeeService.pageQuery(employeePageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 添加员工
     */

    @PostMapping()
    @ApiOperation("添加员工")
    public Result<String> addEmployee(@RequestBody Employee employee) {
        log.info("添加员工：{}", employee);
        employee.setPassword(PasswordConstant.DEFAULT_PASSWORD);
        employeeService.save(employee);
        return Result.success();
    }

    /**
     * 查询员工
     */
    @GetMapping("/{id}")
    @ApiOperation("查询员工")
    public Result<Employee> getEmployee(@PathVariable("id") Long id) {
        log.info("查询员工：{}", id);
        Employee employee = employeeService.getById(id);
        if (Objects.isNull(employee)) {
            return Result.error("员工不存在");
        }
        return Result.success(employee);
    }

    /**
     * 修改员工
     */
    @PutMapping()
    @ApiOperation("修改员工")
    public Result<String> updateEmployee(@RequestBody EmployeeDTO employeeDTO) {
        log.info("修改员工：{}", employeeDTO);
        Employee employee = BeanCopyUtils.copyBean(employeeDTO, Employee.class);
        employeeService.updateById(employee);
        return Result.success();
    }


}
