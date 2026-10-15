package com.smarthotel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smarthotel.dto.auth.ChangePasswordRequest;
import com.smarthotel.dto.auth.BindIdCardRequest;
import com.smarthotel.dto.auth.RegisterRequest;
import com.smarthotel.dto.auth.ResetPasswordRequest;
import com.smarthotel.dto.auth.UpdateProfileRequest;
import com.smarthotel.entity.Customer;
import com.smarthotel.entity.SysRole;
import com.smarthotel.entity.SysUser;
import com.smarthotel.exception.BusinessException;
import com.smarthotel.mapper.CustomerMapper;
import com.smarthotel.mapper.SysRoleMapper;
import com.smarthotel.mapper.SysUserMapper;
import com.smarthotel.vo.CustomerProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerAccountService {
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final CustomerMapper customerMapper;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetRateLimiter passwordResetRateLimiter;

    @Transactional
    public void register(RegisterRequest request) {
        String username = request.username().trim();
        String phone = request.phone().trim();
        if (userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)) != null) {
            throw new BusinessException(409, "账号已存在");
        }
        if (userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhone, phone)) != null
                || customerMapper.selectOne(new LambdaQueryWrapper<Customer>().eq(Customer::getPhone, phone)) != null) {
            throw new BusinessException(409, "手机号已被使用");
        }
        SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, "CUSTOMER"));
        if (role == null) throw new BusinessException(500, "客户角色未配置");
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRoleId(role.getRoleId());
        user.setNickname(request.name().trim());
        user.setPhone(phone);
        user.setStatus("ACTIVE");
        userMapper.insert(user);
        Customer customer = new Customer();
        customer.setUserId(user.getUserId());
        customer.setCustomerName(request.name().trim());
        customer.setPhone(phone);
        customer.setStatus("ACTIVE");
        customer.setRegisterSource("ONLINE");
        customerMapper.insert(customer);
    }

    public CustomerProfileVO profile(Long userId) {
        SysUser user = requireUser(userId);
        Customer customer = requireCustomer(userId);
        return new CustomerProfileVO(userId, user.getUsername(), customer.getCustomerName(), customer.getPhone(),
                customer.getIdNumber() != null && !customer.getIdNumber().isBlank());
    }

    @Transactional
    public CustomerProfileVO updateProfile(Long userId, UpdateProfileRequest request) {
        SysUser user = requireUser(userId);
        Customer customer = requireCustomer(userId);
        String phone = request.phone().trim();
        SysUser phoneUser = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhone, phone));
        Customer phoneCustomer = customerMapper.selectOne(new LambdaQueryWrapper<Customer>().eq(Customer::getPhone, phone));
        if ((phoneUser != null && !userId.equals(phoneUser.getUserId()))
                || (phoneCustomer != null && !customer.getCustomerId().equals(phoneCustomer.getCustomerId()))) {
            throw new BusinessException(409, "手机号已被使用");
        }
        String name = request.name().trim();
        user.setNickname(name);
        user.setPhone(phone);
        customer.setCustomerName(name);
        customer.setPhone(phone);
        userMapper.updateById(user);
        customerMapper.updateById(customer);
        return new CustomerProfileVO(userId, user.getUsername(), name, phone,
                customer.getIdNumber() != null && !customer.getIdNumber().isBlank());
    }

    @Transactional
    public void bindIdCard(Long userId, BindIdCardRequest request) {
        SysUser user = requireUser(userId);
        Customer customer = requireCustomer(userId);
        if (customer.getIdNumber() != null && !customer.getIdNumber().isBlank()) {
            throw new BusinessException(409, "身份证已绑定，如需更改请联系管理员");
        }
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessException(400, "当前密码错误");
        }
        String idNumber = request.idNumber().trim().toUpperCase(java.util.Locale.ROOT);
        if (!IdCardValidator.isValid(idNumber)) {
            throw new BusinessException(400, "请输入有效的18位身份证号");
        }
        customer.setIdType("ID_CARD");
        customer.setIdNumber(idNumber);
        customerMapper.updateById(customer);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request, String ip) {
        String phone = request.phone().trim();
        passwordResetRateLimiter.check(phone, ip == null ? "unknown" : ip);
        String idNumber = request.idNumber().trim().toUpperCase(java.util.Locale.ROOT);
        if (!IdCardValidator.isValid(idNumber)) {
            throw new BusinessException(400, "身份信息不匹配");
        }
        Customer customer = customerMapper.selectOne(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getPhone, phone)
                .eq(Customer::getIdType, "ID_CARD")
                .eq(Customer::getIdNumber, idNumber));
        if (customer == null || customer.getUserId() == null || !"ACTIVE".equals(customer.getStatus())) {
            throw new BusinessException(400, "身份信息不匹配");
        }
        SysUser user = userMapper.selectById(customer.getUserId());
        SysRole role = user == null ? null : roleMapper.selectById(user.getRoleId());
        if (user == null || !"ACTIVE".equals(user.getStatus()) || !phone.equals(user.getPhone())
                || role == null || !"CUSTOMER".equals(role.getRoleCode())) {
            throw new BusinessException(400, "身份信息不匹配");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userMapper.updateById(user);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        SysUser user = requireUser(userId);
        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new BusinessException(400, "原密码错误");
        }
        if (request.oldPassword().equals(request.newPassword())) {
            throw new BusinessException(400, "新密码不能与原密码相同");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userMapper.updateById(user);
    }

    private SysUser requireUser(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null || !"ACTIVE".equals(user.getStatus())) throw new BusinessException(404, "账号不存在");
        return user;
    }

    private Customer requireCustomer(Long userId) {
        Customer customer = customerMapper.selectOne(new LambdaQueryWrapper<Customer>().eq(Customer::getUserId, userId));
        if (customer == null) throw new BusinessException(404, "客户档案不存在");
        return customer;
    }
}
