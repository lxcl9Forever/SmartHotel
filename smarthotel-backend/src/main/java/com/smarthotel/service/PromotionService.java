package com.smarthotel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smarthotel.dto.promotion.PromotionRequest;
import com.smarthotel.entity.Promotion;
import com.smarthotel.exception.BusinessException;
import com.smarthotel.mapper.PromotionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PromotionService {
    private final PromotionMapper mapper;

    public record Price(Long promotionId, BigDecimal originalAmount,
                        BigDecimal discountAmount, BigDecimal payableAmount) {
    }

    public Price calculate(BigDecimal original) {
        BigDecimal originalAmount = original.setScale(2, RoundingMode.HALF_UP);
        Price best = new Price(null, originalAmount, BigDecimal.ZERO.setScale(2), originalAmount);
        LocalDateTime now = LocalDateTime.now();
        List<Promotion> active = mapper.selectList(new LambdaQueryWrapper<Promotion>()
                .eq(Promotion::getStatus, "ACTIVE")
                .le(Promotion::getStartTime, now)
                .ge(Promotion::getEndTime, now)
                .orderByAsc(Promotion::getId));
        for (Promotion promotion : active) {
            BigDecimal payable;
            if ("DISCOUNT".equals(promotion.getType()) && promotion.getDiscountRate() != null) {
                payable = originalAmount.multiply(promotion.getDiscountRate())
                        .setScale(2, RoundingMode.HALF_UP);
            } else if ("FULL_REDUCTION".equals(promotion.getType())
                    && promotion.getMinAmount() != null && promotion.getReduceAmount() != null
                    && originalAmount.compareTo(promotion.getMinAmount()) >= 0) {
                payable = originalAmount.subtract(promotion.getReduceAmount());
            } else {
                continue;
            }
            if (payable.compareTo(new BigDecimal("0.01")) >= 0
                    && payable.compareTo(best.payableAmount()) < 0) {
                best = new Price(promotion.getId(), originalAmount,
                        originalAmount.subtract(payable), payable);
            }
        }
        return best;
    }

    @Transactional
    public Promotion create(PromotionRequest request) {
        validate(request);
        Promotion promotion = fromRequest(request);
        promotion.setStatus("INACTIVE");
        mapper.insert(promotion);
        return promotion;
    }

    @Transactional
    public Promotion update(Long id, PromotionRequest request) {
        validate(request);
        Promotion existing = requireExisting(id);
        Promotion changed = fromRequest(request);
        changed.setId(id);
        changed.setStatus(existing.getStatus());
        mapper.updateById(changed);
        return mapper.selectById(id);
    }

    @Transactional
    public void delete(Long id) {
        Promotion promotion = requireExisting(id);
        promotion.setStatus("DELETED");
        mapper.updateById(promotion);
    }

    @Transactional
    public void setEnabled(Long id, boolean enabled) {
        Promotion promotion = requireExisting(id);
        promotion.setStatus(enabled ? "ACTIVE" : "INACTIVE");
        mapper.updateById(promotion);
    }

    public List<Promotion> list() {
        return mapper.selectList(new LambdaQueryWrapper<Promotion>()
                .ne(Promotion::getStatus, "DELETED")
                .orderByDesc(Promotion::getId));
    }

    private Promotion requireExisting(Long id) {
        Promotion promotion = mapper.selectById(id);
        if (promotion == null || "DELETED".equals(promotion.getStatus())) {
            throw new BusinessException(404, "优惠活动不存在");
        }
        return promotion;
    }

    private Promotion fromRequest(PromotionRequest request) {
        Promotion promotion = new Promotion();
        promotion.setName(request.getName().trim());
        promotion.setType(request.getType());
        promotion.setDiscountRate(request.getDiscountRate());
        promotion.setMinAmount(request.getMinAmount());
        promotion.setReduceAmount(request.getReduceAmount());
        promotion.setStartTime(request.getStartTime());
        promotion.setEndTime(request.getEndTime());
        return promotion;
    }

    private void validate(PromotionRequest request) {
        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new BusinessException(400, "活动开始时间必须早于结束时间");
        }
        if ("DISCOUNT".equals(request.getType())) {
            if (request.getDiscountRate() == null
                    || request.getDiscountRate().compareTo(BigDecimal.ZERO) <= 0
                    || request.getDiscountRate().compareTo(BigDecimal.ONE) >= 0
                    || request.getDiscountRate().scale() > 4) {
                throw new BusinessException(400, "折扣率必须大于0且小于1，最多4位小数");
            }
        } else if ("FULL_REDUCTION".equals(request.getType())) {
            if (request.getMinAmount() == null || request.getReduceAmount() == null
                    || request.getMinAmount().compareTo(BigDecimal.ZERO) <= 0
                    || request.getReduceAmount().compareTo(BigDecimal.ZERO) <= 0
                    || request.getMinAmount().scale() > 2
                    || request.getReduceAmount().scale() > 2
                    || request.getReduceAmount().compareTo(request.getMinAmount()) >= 0) {
                throw new BusinessException(400, "满减金额必须大于0且小于门槛金额");
            }
        } else {
            throw new BusinessException(400, "优惠类型只能是 DISCOUNT 或 FULL_REDUCTION");
        }
    }
}
