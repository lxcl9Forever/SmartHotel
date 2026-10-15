package com.smarthotel.service;

import com.smarthotel.dto.ai.RecommendRequest;
import com.smarthotel.dto.ai.RoomPreference;
import com.smarthotel.entity.RoomInventory;
import com.smarthotel.entity.RoomPrice;
import com.smarthotel.entity.RoomType;
import com.smarthotel.exception.BusinessException;
import com.smarthotel.mapper.RoomInventoryMapper;
import com.smarthotel.mapper.RoomPriceMapper;
import com.smarthotel.mapper.RoomRatingMapper;
import com.smarthotel.mapper.RoomTypeMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AIRecommendServiceTest {
    private final LLMClient llm = mock(LLMClient.class);
    private final RoomTypeMapper rooms = mock(RoomTypeMapper.class);
    private final RoomInventoryMapper inventory = mock(RoomInventoryMapper.class);
    private final RoomPriceMapper prices = mock(RoomPriceMapper.class);
    private final RoomRatingMapper ratings = mock(RoomRatingMapper.class);
    private final PromotionService promotions = mock(PromotionService.class);
    private final AIRecommendService service = new AIRecommendService(
            llm, rooms, inventory, prices, ratings, promotions);

    @Test
    void doesNotFallBackWhenModelIsUnavailable() {
        when(llm.parse(any())).thenThrow(new BusinessException(502, "智能选房模型调用失败"));
        RecommendRequest request = new RecommendRequest();
        request.setText("一家三口入住三晚，预算1000，希望安静");
        assertThrows(BusinessException.class, () -> service.recommend(request));
        verifyNoInteractions(rooms, inventory, prices);
    }

    @Test
    void recommendsOnlyWhenEachNightHasInventoryAndPriceWithinBudget() {
        LocalDate firstDay = LocalDate.now().plusDays(1);
        when(llm.parse(any())).thenReturn(new LLMClient.Interpretation(
                new RoomPreference(3, 2, new BigDecimal("700"), "安静", "LLM"),
                "您好，感谢您告诉我入住需求。"));
        RoomType room = new RoomType();
        room.setRoomTypeId(1L);
        room.setTypeName("安静家庭房");
        room.setMaxGuests(3);
        when(rooms.selectList(any())).thenReturn(List.of(room));
        RoomInventory first = new RoomInventory();
        first.setTotalQty(2); first.setLockedQty(1); first.setSoldQty(0);
        RoomInventory second = new RoomInventory();
        second.setTotalQty(2); second.setLockedQty(0); second.setSoldQty(1);
        when(inventory.selectList(any())).thenReturn(List.of(first, second));
        RoomPrice firstPrice = new RoomPrice();
        firstPrice.setSalePrice(new BigDecimal("300"));
        RoomPrice secondPrice = new RoomPrice();
        secondPrice.setSalePrice(new BigDecimal("350"));
        when(prices.selectList(any())).thenReturn(List.of(firstPrice, secondPrice));
        when(promotions.calculate(new BigDecimal("650")))
                .thenReturn(new PromotionService.Price(null, new BigDecimal("650"),
                        BigDecimal.ZERO, new BigDecimal("650")));
        RecommendRequest request = new RecommendRequest();
        request.setText("3人2晚，预算700，希望安静");
        request.setCheckInDate(firstDay);

        var result = service.recommend(request);
        assertEquals(1, result.recommendations().size());
        assertEquals(new BigDecimal("650"), result.recommendations().get(0).totalAmount());
        assertEquals(1, result.recommendations().get(0).availableQty());
        assertEquals(firstDay.plusDays(2), result.checkOutDate());
        assertTrue(result.reply().startsWith("您好，感谢您告诉我入住需求。"));

        second.setSoldQty(2);
        assertTrue(service.recommend(request).recommendations().isEmpty());
    }

    @Test
    void suggestsClosestAvailableRoomAndOneNightShorterPriceWhenBudgetIsTooLow() {
        LocalDate checkIn = LocalDate.now().plusDays(1);
        when(llm.parse(any())).thenReturn(new LLMClient.Interpretation(
                new RoomPreference(3, 2, new BigDecimal("600"), "", "LLM"),
                "您好，感谢您的耐心等待。"));
        RoomType room = new RoomType();
        room.setRoomTypeId(7L);
        room.setTypeName("家庭房");
        room.setMaxGuests(3);
        RoomType expensiveRoom = new RoomType();
        expensiveRoom.setRoomTypeId(8L);
        expensiveRoom.setTypeName("豪华家庭房");
        expensiveRoom.setMaxGuests(3);
        when(rooms.selectList(any())).thenReturn(List.of(expensiveRoom, room));
        RoomInventory first = new RoomInventory();
        first.setTotalQty(1); first.setLockedQty(0); first.setSoldQty(0);
        RoomInventory second = new RoomInventory();
        second.setTotalQty(1); second.setLockedQty(0); second.setSoldQty(0);
        when(inventory.selectList(any())).thenReturn(List.of(first, second));
        RoomPrice firstPrice = new RoomPrice();
        firstPrice.setPriceDate(checkIn);
        firstPrice.setSalePrice(new BigDecimal("450"));
        RoomPrice secondPrice = new RoomPrice();
        secondPrice.setPriceDate(checkIn.plusDays(1));
        secondPrice.setSalePrice(new BigDecimal("500"));
        RoomPrice expensivePrice = new RoomPrice();
        expensivePrice.setSalePrice(new BigDecimal("600"));
        when(prices.selectList(any())).thenReturn(
                List.of(expensivePrice, expensivePrice), List.of(firstPrice, secondPrice));
        when(promotions.calculate(any())).thenAnswer(call -> {
            BigDecimal amount = call.getArgument(0);
            return new PromotionService.Price(null, amount, BigDecimal.ZERO, amount);
        });
        RecommendRequest request = new RecommendRequest();
        request.setText("3人住2晚，预算600");
        request.setCheckInDate(checkIn);

        var result = service.recommend(request);
        assertTrue(result.recommendations().isEmpty());
        assertNotNull(result.budgetAlternative());
        assertEquals(7L, result.budgetAlternative().room().roomTypeId());
        assertEquals(new BigDecimal("350"), result.budgetAlternative().additionalBudget());
        assertEquals(new BigDecimal("450"), result.budgetAlternative().shorterTotalAmount());
        assertEquals(checkIn.plusDays(1), result.budgetAlternative().shorterCheckOutDate());
        assertTrue(result.reply().contains("增加¥350"));
        assertTrue(result.reply().contains("可以控制在您的预算内"));

        when(llm.parse(any())).thenReturn(new LLMClient.Interpretation(
                new RoomPreference(3, 2, new BigDecimal("300"), "", "LLM"),
                "您好，感谢您的耐心等待。"));
        when(prices.selectList(any())).thenReturn(
                List.of(expensivePrice, expensivePrice), List.of(firstPrice, secondPrice));
        var lowerBudget = service.recommend(request);
        assertTrue(lowerBudget.reply().contains("仍比预算多¥150"));
    }
}
