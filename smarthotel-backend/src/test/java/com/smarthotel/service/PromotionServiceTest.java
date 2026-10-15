package com.smarthotel.service;

import com.smarthotel.entity.Promotion;
import com.smarthotel.mapper.PromotionMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PromotionServiceTest {
    private final PromotionMapper mapper = mock(PromotionMapper.class);
    private final PromotionService service = new PromotionService(mapper);

    @Test
    void choosesBestEligiblePromotionWithoutStacking() {
        Promotion discount = new Promotion();
        discount.setId(1L);
        discount.setType("DISCOUNT");
        discount.setDiscountRate(new BigDecimal("0.80"));
        Promotion fullReduction = new Promotion();
        fullReduction.setId(2L);
        fullReduction.setType("FULL_REDUCTION");
        fullReduction.setMinAmount(new BigDecimal("500"));
        fullReduction.setReduceAmount(new BigDecimal("150"));
        when(mapper.selectList(any())).thenReturn(List.of(discount, fullReduction));

        PromotionService.Price price = service.calculate(new BigDecimal("600.00"));

        assertEquals(2L, price.promotionId());
        assertEquals(new BigDecimal("600.00"), price.originalAmount());
        assertEquals(new BigDecimal("150.00"), price.discountAmount());
        assertEquals(new BigDecimal("450.00"), price.payableAmount());
    }

    @Test
    void returnsOriginalPriceWhenNoPromotionApplies() {
        when(mapper.selectList(any())).thenReturn(List.of());
        PromotionService.Price price = service.calculate(new BigDecimal("300.00"));
        assertNull(price.promotionId());
        assertEquals(new BigDecimal("300.00"), price.payableAmount());
    }
}
