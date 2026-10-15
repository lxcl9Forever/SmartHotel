package com.smarthotel.service;

import com.smarthotel.dto.auth.SavedGuestRequest;
import com.smarthotel.entity.CustomerSavedGuest;
import com.smarthotel.exception.BusinessException;
import com.smarthotel.mapper.CustomerSavedGuestMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CustomerSavedGuestServiceTest {
    private final CustomerSavedGuestMapper mapper = mock(CustomerSavedGuestMapper.class);
    private final CustomerSavedGuestService service = new CustomerSavedGuestService(mapper);

    @Test
    void createsGuestUnderAuthenticatedUserAndNormalizesFields() {
        when(mapper.selectCount(any())).thenReturn(0L);
        when(mapper.insert(any(CustomerSavedGuest.class))).thenAnswer(call -> {
            CustomerSavedGuest guest = call.getArgument(0);
            guest.setGuestId(5L);
            return 1;
        });
        when(mapper.selectById(5L)).thenReturn(new CustomerSavedGuest());

        service.create(42L, new SavedGuestRequest(" 张三 ", "13812345678", "ID_CARD", " 123456789 "));

        ArgumentCaptor<CustomerSavedGuest> guestCaptor = ArgumentCaptor.forClass(CustomerSavedGuest.class);
        verify(mapper).insert(guestCaptor.capture());
        assertEquals(42L, guestCaptor.getValue().getUserId());
        assertEquals("张三", guestCaptor.getValue().getGuestName());
        assertEquals("123456789", guestCaptor.getValue().getIdNumber());
    }

    @Test
    void refusesToEditOrDeleteGuestOutsideCurrentAccount() {
        when(mapper.selectOne(any())).thenReturn(null);
        SavedGuestRequest request = new SavedGuestRequest("李四", "13912345678", "PASSPORT", null);

        assertThrows(BusinessException.class, () -> service.update(42L, 99L, request));
        assertThrows(BusinessException.class, () -> service.delete(42L, 99L));
        verify(mapper, never()).updateById(any(CustomerSavedGuest.class));
        verify(mapper, never()).delete(any());
    }
}
