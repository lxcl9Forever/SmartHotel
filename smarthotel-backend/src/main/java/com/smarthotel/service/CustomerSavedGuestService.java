package com.smarthotel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smarthotel.dto.auth.SavedGuestRequest;
import com.smarthotel.entity.CustomerSavedGuest;
import com.smarthotel.exception.BusinessException;
import com.smarthotel.mapper.CustomerSavedGuestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerSavedGuestService {
    private static final int MAX_SAVED_GUESTS = 20;
    private final CustomerSavedGuestMapper mapper;

    public List<CustomerSavedGuest> list(Long userId) {
        return mapper.selectList(new LambdaQueryWrapper<CustomerSavedGuest>()
                .eq(CustomerSavedGuest::getUserId, userId)
                .orderByDesc(CustomerSavedGuest::getUpdateTime)
                .orderByDesc(CustomerSavedGuest::getGuestId));
    }

    @Transactional
    public CustomerSavedGuest create(Long userId, SavedGuestRequest request) {
        if (mapper.selectCount(new LambdaQueryWrapper<CustomerSavedGuest>()
                .eq(CustomerSavedGuest::getUserId, userId)) >= MAX_SAVED_GUESTS) {
            throw new BusinessException(400, "最多保存20位常用入住人");
        }
        CustomerSavedGuest guest = new CustomerSavedGuest();
        guest.setUserId(userId);
        apply(guest, request);
        mapper.insert(guest);
        return mapper.selectById(guest.getGuestId());
    }

    @Transactional
    public CustomerSavedGuest update(Long userId, Long guestId, SavedGuestRequest request) {
        CustomerSavedGuest guest = requireOwned(userId, guestId);
        apply(guest, request);
        mapper.updateById(guest);
        return requireOwned(userId, guestId);
    }

    @Transactional
    public void delete(Long userId, Long guestId) {
        requireOwned(userId, guestId);
        mapper.delete(new LambdaQueryWrapper<CustomerSavedGuest>()
                .eq(CustomerSavedGuest::getGuestId, guestId)
                .eq(CustomerSavedGuest::getUserId, userId));
    }

    private CustomerSavedGuest requireOwned(Long userId, Long guestId) {
        CustomerSavedGuest guest = mapper.selectOne(new LambdaQueryWrapper<CustomerSavedGuest>()
                .eq(CustomerSavedGuest::getGuestId, guestId)
                .eq(CustomerSavedGuest::getUserId, userId));
        if (guest == null) throw new BusinessException(404, "常用入住人不存在");
        return guest;
    }

    private void apply(CustomerSavedGuest guest, SavedGuestRequest request) {
        guest.setGuestName(request.guestName().trim());
        guest.setPhone(request.phone().trim());
        guest.setIdType(request.idType());
        guest.setIdNumber(request.idNumber() == null || request.idNumber().isBlank()
                ? null : request.idNumber().trim());
    }
}
