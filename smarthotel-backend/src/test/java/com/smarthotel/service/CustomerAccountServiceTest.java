package com.smarthotel.service;

import com.smarthotel.dto.auth.ChangePasswordRequest;
import com.smarthotel.dto.auth.BindIdCardRequest;
import com.smarthotel.dto.auth.RegisterRequest;
import com.smarthotel.dto.auth.ResetPasswordRequest;
import com.smarthotel.entity.Customer;
import com.smarthotel.entity.SysRole;
import com.smarthotel.entity.SysUser;
import com.smarthotel.exception.BusinessException;
import com.smarthotel.mapper.CustomerMapper;
import com.smarthotel.mapper.SysRoleMapper;
import com.smarthotel.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CustomerAccountServiceTest {
    private final SysUserMapper users = mock(SysUserMapper.class);
    private final SysRoleMapper roles = mock(SysRoleMapper.class);
    private final CustomerMapper customers = mock(CustomerMapper.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final PasswordResetRateLimiter limiter = mock(PasswordResetRateLimiter.class);
    private final CustomerAccountService service = new CustomerAccountService(users, roles, customers, encoder, limiter);

    @Test
    void registrationCreatesLoginAndCustomerProfile() {
        SysRole role = new SysRole(); role.setRoleId(3L);
        when(roles.selectOne(any())).thenReturn(role);
        when(encoder.encode("strongpass")).thenReturn("hash");
        doAnswer(invocation -> {
            SysUser user = invocation.getArgument(0);
            user.setUserId(9L);
            return 1;
        }).when(users).insert(any(SysUser.class));

        service.register(new RegisterRequest("newcustomer", "strongpass", "顾客", "13800138000"));

        ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(users).insert(userCaptor.capture());
        verify(customers).insert(customerCaptor.capture());
        assertEquals(3L, userCaptor.getValue().getRoleId());
        assertEquals("hash", userCaptor.getValue().getPassword());
        assertTrue(!"strongpass".equals(userCaptor.getValue().getPassword()));
        assertEquals(9L, customerCaptor.getValue().getUserId());
        assertEquals("13800138000", customerCaptor.getValue().getPhone());
    }

    @Test
    void wrongCurrentPasswordDoesNotChangePassword() {
        SysUser user = new SysUser(); user.setStatus("ACTIVE"); user.setPassword("stored");
        when(users.selectById(9L)).thenReturn(user);
        when(encoder.matches("wrong", "stored")).thenReturn(false);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.changePassword(9L, new ChangePasswordRequest("wrong", "newstrongpass")));

        assertEquals(400, error.getCode());
        verify(users, never()).updateById(any(SysUser.class));
    }

    @Test
    void mismatchedIdentityCannotResetPassword() {
        when(customers.selectOne(any())).thenReturn(null);
        BusinessException error = assertThrows(BusinessException.class,
                () -> service.resetPassword(new ResetPasswordRequest(
                        "13800138000", "11010519491231002X", "newstrongpass"), "127.0.0.1"));
        assertEquals(400, error.getCode());
        verify(users, never()).updateById(any(SysUser.class));
    }

    @Test
    void matchingCustomerIdentityResetsOnlyTheCustomerPassword() {
        Customer customer = new Customer();
        customer.setUserId(9L);
        customer.setStatus("ACTIVE");
        when(customers.selectOne(any())).thenReturn(customer);
        SysUser user = new SysUser();
        user.setUserId(9L);
        user.setPhone("13800138000");
        user.setStatus("ACTIVE");
        user.setRoleId(3L);
        when(users.selectById(9L)).thenReturn(user);
        SysRole role = new SysRole();
        role.setRoleCode("CUSTOMER");
        when(roles.selectById(3L)).thenReturn(role);
        when(encoder.encode("newstrongpass")).thenReturn("new-hash");

        service.resetPassword(new ResetPasswordRequest(
                "13800138000", "11010519491231002X", "newstrongpass"), "127.0.0.1");

        verify(limiter).check("13800138000", "127.0.0.1");
        assertEquals("new-hash", user.getPassword());
        verify(users).updateById(user);
    }

    @Test
    void bindingIdCardRequiresCurrentPassword() {
        SysUser user = new SysUser();
        user.setStatus("ACTIVE");
        user.setPassword("stored");
        when(users.selectById(9L)).thenReturn(user);
        when(customers.selectOne(any())).thenReturn(new Customer());
        when(encoder.matches("wrong", "stored")).thenReturn(false);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.bindIdCard(9L, new BindIdCardRequest("11010519491231002X", "wrong")));

        assertEquals(400, error.getCode());
        verify(customers, never()).updateById(any(Customer.class));
    }
}
