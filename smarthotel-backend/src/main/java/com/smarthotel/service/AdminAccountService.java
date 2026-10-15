package com.smarthotel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smarthotel.dto.admin.CreateEmployeeRequest;
import com.smarthotel.dto.admin.UpdateEmployeeRequest;
import com.smarthotel.entity.Employee;
import com.smarthotel.entity.SysRole;
import com.smarthotel.entity.SysUser;
import com.smarthotel.exception.BusinessException;
import com.smarthotel.mapper.AdminPaymentQueryMapper;
import com.smarthotel.mapper.EmployeeMapper;
import com.smarthotel.mapper.SysRoleMapper;
import com.smarthotel.mapper.SysUserMapper;
import com.smarthotel.vo.AdminEmployeeVO;
import com.smarthotel.vo.AdminPaymentVO;
import com.smarthotel.vo.AdminUserVO;
import com.smarthotel.vo.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminAccountService {
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final EmployeeMapper employeeMapper;
    private final AdminPaymentQueryMapper paymentMapper;
    private final PasswordEncoder passwordEncoder;

    public PageResult<AdminUserVO> users(Integer page, Integer pageSize) {
        int p = safePage(page), size = safeSize(pageSize);
        long total = userMapper.selectCount(null);
        Map<Long, String> roles = roleMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysRole::getRoleId, SysRole::getRoleCode));
        List<AdminUserVO> records = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .orderByDesc(SysUser::getUserId).last("LIMIT " + ((long) (p - 1) * size) + ", " + size))
                .stream().map(u -> new AdminUserVO(u.getUserId(), u.getUsername(), u.getNickname(),
                        u.getPhone(), roles.get(u.getRoleId()), u.getStatus())).toList();
        return new PageResult<>(total, p, size, records);
    }

    @Transactional
    public void setUserStatus(Long userId, String status, Long operatorId) {
        if (!"ACTIVE".equals(status) && !"DISABLED".equals(status)) throw new BusinessException(400, "无效的账号状态");
        if (userId.equals(operatorId)) throw new BusinessException(409, "不能停用自己的账号");
        SysUser user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(404, "用户不存在");
        user.setStatus(status);
        userMapper.updateById(user);
        Employee employee = employeeMapper.selectOne(new LambdaQueryWrapper<Employee>().eq(Employee::getUserId, userId));
        if (employee != null) {
            employee.setStatus("ACTIVE".equals(status) ? "ACTIVE" : "INACTIVE");
            employeeMapper.updateById(employee);
        }
    }

    public PageResult<AdminEmployeeVO> employees(Integer page, Integer pageSize) {
        int p = safePage(page), size = safeSize(pageSize);
        long total = employeeMapper.selectCount(null);
        List<AdminEmployeeVO> records = employeeMapper.selectList(new LambdaQueryWrapper<Employee>()
                .orderByDesc(Employee::getEmployeeId).last("LIMIT " + ((long) (p - 1) * size) + ", " + size))
                .stream().map(e -> {
                    SysUser user = userMapper.selectById(e.getUserId());
                    return new AdminEmployeeVO(e.getEmployeeId(), e.getUserId(), user == null ? "" : user.getUsername(),
                            e.getEmployeeNo(), e.getEmployeeName(), e.getPhone(), e.getEmail(), e.getPosition(), e.getStatus());
                }).toList();
        return new PageResult<>(total, p, size, records);
    }

    @Transactional
    public void createEmployee(CreateEmployeeRequest request) {
        String username = request.username().trim();
        String employeeNo = request.employeeNo().trim();
        if (userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)) != null)
            throw new BusinessException(409, "账号已存在");
        if (employeeMapper.selectOne(new LambdaQueryWrapper<Employee>().eq(Employee::getEmployeeNo, employeeNo)) != null)
            throw new BusinessException(409, "员工工号已存在");
        if (request.phone() != null && !request.phone().isBlank()
                && userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhone, request.phone().trim())) != null)
            throw new BusinessException(409, "手机号已被使用");
        SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, "STAFF"));
        if (role == null) throw new BusinessException(500, "员工角色未配置");
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRoleId(role.getRoleId());
        user.setNickname(request.employeeName().trim());
        user.setPhone(request.phone() == null || request.phone().isBlank() ? null : request.phone().trim());
        user.setStatus("ACTIVE");
        userMapper.insert(user);
        Employee employee = new Employee();
        employee.setUserId(user.getUserId());
        employee.setEmployeeNo(employeeNo);
        employee.setEmployeeName(request.employeeName().trim());
        employee.setPhone(user.getPhone());
        employee.setEmail(request.email());
        employee.setPosition(request.position());
        employee.setStatus("ACTIVE");
        employeeMapper.insert(employee);
    }

    public PageResult<AdminPaymentVO> payments(Integer page, Integer pageSize) {
        int p = safePage(page), size = safeSize(pageSize);
        return new PageResult<>(paymentMapper.countAll(), p, size,
                paymentMapper.list((long) (p - 1) * size, size));
    }

    @Transactional
    public void updateEmployee(Long employeeId, UpdateEmployeeRequest request) {
        Employee employee = employeeMapper.selectById(employeeId);
        if (employee == null) throw new BusinessException(404, "员工不存在");
        SysUser user = userMapper.selectById(employee.getUserId());
        if (user == null) throw new BusinessException(404, "员工账号不存在");
        String phone = request.phone() == null || request.phone().isBlank() ? null : request.phone().trim();
        if (phone != null) {
            SysUser existing = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhone, phone));
            if (existing != null && !existing.getUserId().equals(user.getUserId()))
                throw new BusinessException(409, "手机号已被使用");
        }
        employee.setEmployeeName(request.employeeName().trim());
        employee.setPhone(phone);
        employee.setEmail(request.email());
        employee.setPosition(request.position());
        user.setNickname(employee.getEmployeeName());
        user.setPhone(phone);
        employeeMapper.updateById(employee);
        userMapper.updateById(user);
    }

    private int safePage(Integer page) { return page == null ? 1 : Math.max(1, page); }
    private int safeSize(Integer size) { return size == null ? 10 : Math.max(1, Math.min(size, 100)); }
}
