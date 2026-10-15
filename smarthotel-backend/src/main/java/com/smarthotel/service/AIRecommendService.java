package com.smarthotel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.smarthotel.vo.AIRecommendationVO;
import com.smarthotel.vo.BudgetAlternativeVO;
import com.smarthotel.vo.RecommendedRoomVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AIRecommendService {
    private final LLMClient llmClient;
    private final RoomTypeMapper roomTypeMapper;
    private final RoomInventoryMapper inventoryMapper;
    private final RoomPriceMapper priceMapper;
    private final RoomRatingMapper ratingMapper;
    private final PromotionService promotionService;

    public AIRecommendationVO recommend(RecommendRequest request) {
        LLMClient.Interpretation interpretation = llmClient.parse(request.getText());
        RoomPreference preference = interpretation.preference();
        LocalDate checkIn = request.getCheckInDate() == null
                ? LocalDate.now().plusDays(1) : request.getCheckInDate();
        if (checkIn.isBefore(LocalDate.now())) throw new BusinessException(400, "入住日期不能早于今天");
        LocalDate checkOut = checkIn.plusDays(preference.days());
        List<RoomType> roomTypes = roomTypeMapper.selectList(new LambdaQueryWrapper<RoomType>()
                .eq(RoomType::getStatus, 1));
        List<ScoredRoom> candidates = new ArrayList<>();
        List<BudgetOption> overBudget = new ArrayList<>();

        for (RoomType room : roomTypes) {
            if (room.getMaxGuests() == null || room.getMaxGuests() < preference.people()) continue;
            List<RoomInventory> inventory = inventoryMapper.selectList(new LambdaQueryWrapper<RoomInventory>()
                    .eq(RoomInventory::getRoomTypeId, room.getRoomTypeId())
                    .ge(RoomInventory::getInventoryDate, checkIn)
                    .lt(RoomInventory::getInventoryDate, checkOut));
            if (inventory.size() != preference.days()) continue;
            int available = inventory.stream().mapToInt(day -> day.getTotalQty()
                    - day.getLockedQty() - day.getSoldQty()).min().orElse(0);
            if (available < 1) continue;
            List<RoomPrice> prices = priceMapper.selectList(new LambdaQueryWrapper<RoomPrice>()
                    .eq(RoomPrice::getRoomTypeId, room.getRoomTypeId())
                    .ge(RoomPrice::getPriceDate, checkIn)
                    .lt(RoomPrice::getPriceDate, checkOut)
                    .orderByAsc(RoomPrice::getPriceDate));
            if (prices.size() != preference.days()) continue;
            BigDecimal original = prices.stream().map(RoomPrice::getSalePrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal total = promotionService.calculate(original).payableAmount();

            BigDecimal rating = ratingMapper.averageRating(room.getRoomTypeId());
            boolean preferenceMatched = matchesPreference(room, preference.preference());
            String reason = "适合" + preference.people() + "人入住，" + preference.days()
                    + "晚约¥" + total + "，当前至少有" + available + "间可售"
                    + (rating == null ? "" : "，评价均分" + rating.setScale(1, RoundingMode.HALF_UP))
                    + (preference.preference().isBlank() ? "。"
                    : preferenceMatched ? "，符合“" + preference.preference() + "”偏好。"
                    : "；“" + preference.preference() + "”偏好需向酒店确认。");
            ScoredRoom scored = new ScoredRoom(preferenceMatched,
                    rating == null ? BigDecimal.ZERO : rating,
                    new RecommendedRoomVO(room.getRoomTypeId(), room.getTypeName(), total,
                            available, rating, reason));
            if (preference.budget() != null && total.compareTo(preference.budget()) > 0) {
                BigDecimal shorterTotal = null;
                if (preference.days() > 1) {
                    BigDecimal shorterOriginal = prices.stream().limit(preference.days() - 1L)
                            .map(RoomPrice::getSalePrice).reduce(BigDecimal.ZERO, BigDecimal::add);
                    shorterTotal = promotionService.calculate(shorterOriginal).payableAmount();
                }
                overBudget.add(new BudgetOption(scored, shorterTotal));
            } else {
                candidates.add(scored);
            }
        }
        candidates.sort(Comparator.comparing(ScoredRoom::preferenceMatched).reversed()
                .thenComparing(ScoredRoom::rating, Comparator.reverseOrder())
                .thenComparing(candidate -> candidate.room().totalAmount())
                .thenComparing(candidate -> candidate.room().roomTypeId()));
        List<RecommendedRoomVO> recommendations = candidates.stream().limit(3)
                .map(ScoredRoom::room).toList();
        BudgetAlternativeVO alternative = null;
        String reply;
        if (!recommendations.isEmpty()) {
            reply = "根据您" + preference.people() + "人入住" + preference.days() + "晚的需求"
                    + (preference.budget() == null ? "" : "和¥" + preference.budget() + "的预算")
                    + "，我找到了下面" + recommendations.size() + "个有库存的房型。"
                    + (preference.preference().isBlank() ? "" : "关于“" + preference.preference()
                    + "”偏好，请留意每个房型的说明。选择喜欢的房型即可填写预订信息。");
        } else if (!overBudget.isEmpty()) {
            BudgetOption closest = overBudget.stream()
                    .min(Comparator.comparing(option -> option.scored().room().totalAmount()))
                    .orElseThrow();
            RecommendedRoomVO room = closest.scored().room();
            BigDecimal gap = room.totalAmount().subtract(preference.budget());
            LocalDate shorterCheckOut = preference.days() > 1 ? checkOut.minusDays(1) : null;
            alternative = new BudgetAlternativeVO(room, gap, shorterCheckOut, closest.shorterTotal());
            reply = "抱歉，按您" + preference.people() + "人入住" + preference.days() + "晚、预算¥"
                    + preference.budget() + "的条件，目前没有预算内的房型。离预算最近的可订房型是下方的“"
                    + room.roomTypeName() + "”，全程¥" + room.totalAmount() + "。若保持原入住晚数，"
                    + "增加¥" + gap + "即可预订。";
            if (closest.shorterTotal() != null) {
                BigDecimal shorterGap = closest.shorterTotal().subtract(preference.budget());
                reply += "若少住一晚（" + (preference.days() - 1) + "晚），当前价格为¥"
                        + closest.shorterTotal() + (shorterGap.signum() <= 0
                        ? "，可以控制在您的预算内。"
                        : "，仍比预算多¥" + shorterGap + "。您也可以调整预算后预订。");
            }
        } else {
            reply = "您选择的日期里，暂时没有同时满足入住人数、逐日库存和房价条件的房型。"
                    + "请尝试调整入住日期或人数。";
        }
        return new AIRecommendationVO(preference, checkIn, checkOut,
                recommendations, interpretation.greeting() + " " + reply, alternative);
    }

    private boolean matchesPreference(RoomType room, String preference) {
        if (preference.isBlank()) return false;
        String details = (room.getTypeName() + " " + room.getDescription()).toLowerCase();
        if ("安静".equals(preference)) return details.contains("安静") || details.contains("静谧");
        if ("家庭".equals(preference)) return details.contains("家庭") || details.contains("亲子");
        if ("景观".equals(preference)) return details.contains("景观") || details.contains("观景");
        return details.contains(preference.toLowerCase());
    }

    private record ScoredRoom(boolean preferenceMatched, BigDecimal rating, RecommendedRoomVO room) {
    }

    private record BudgetOption(ScoredRoom scored, BigDecimal shorterTotal) {
    }
}
