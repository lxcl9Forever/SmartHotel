package com.smarthotel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smarthotel.dto.review.CreateReviewRequest;
import com.smarthotel.entity.BookingOrder;
import com.smarthotel.entity.Customer;
import com.smarthotel.exception.BusinessException;
import com.smarthotel.mapper.BookingOrderMapper;
import com.smarthotel.mapper.CustomerMapper;
import com.smarthotel.mapper.OrderReviewMapper;
import com.smarthotel.service.OrderReviewService;
import com.smarthotel.vo.OrderReviewVO;
import com.smarthotel.vo.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OrderReviewServiceImpl implements OrderReviewService {
    private static final Set<String> REVIEWABLE = Set.of("CHECKED_OUT", "COMPLETED");
    private static final Set<String> STATUSES = Set.of("VISIBLE", "HIDDEN");

    private final OrderReviewMapper reviewMapper;
    private final BookingOrderMapper bookingOrderMapper;
    private final CustomerMapper customerMapper;

    @Override
    @Transactional
    public void create(Long orderId, Long userId, CreateReviewRequest request) {
        if (request == null || request.getRating() == null || request.getRating() < 1
                || request.getRating() > 5 || request.getContent() == null
                || request.getContent().trim().isEmpty() || request.getContent().trim().length() > 500) {
            throw new BusinessException(400, "请填写1到5星评价和不超过500字的内容");
        }
        BookingOrder order = ownedOrder(orderId, userId, true);
        if (!REVIEWABLE.contains(order.getOrderStatus())) {
            throw new BusinessException(409, "订单退房后才能评价");
        }
        if (reviewMapper.selectByOrderId(orderId) != null) {
            throw new BusinessException(409, "该订单已经评价");
        }
        reviewMapper.insert(orderId, order.getCustomerId(), request.getRating(), request.getContent().trim());
    }

    @Override
    public OrderReviewVO getMine(Long orderId, Long userId) {
        ownedOrder(orderId, userId, false);
        return reviewMapper.selectByOrderId(orderId);
    }

    @Override
    public PageResult<OrderReviewVO> listForAdmin(String status, Integer page, Integer pageSize) {
        String filter = status == null || status.isBlank() ? null : status.trim().toUpperCase();
        if (filter != null && !STATUSES.contains(filter)) {
            throw new BusinessException(400, "无效的评价状态");
        }
        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        long total = reviewMapper.countForAdmin(filter);
        List<OrderReviewVO> records = total == 0 ? List.of()
                : reviewMapper.listForAdmin(filter, (long) (safePage - 1) * safePageSize, safePageSize);
        return new PageResult<>(total, safePage, safePageSize, records);
    }

    @Override
    public void updateStatus(Long reviewId, String status) {
        if (!STATUSES.contains(status)) {
            throw new BusinessException(400, "无效的评价状态");
        }
        if (reviewMapper.updateStatus(reviewId, status) != 1) {
            throw new BusinessException(404, "评价不存在");
        }
    }

    private BookingOrder ownedOrder(Long orderId, Long userId, boolean lock) {
        Customer customer = customerMapper.selectOne(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getUserId, userId));
        if (customer == null) {
            throw new BusinessException(404, "客户档案不存在");
        }
        LambdaQueryWrapper<BookingOrder> query = new LambdaQueryWrapper<BookingOrder>()
                .eq(BookingOrder::getOrderId, orderId)
                .eq(BookingOrder::getCustomerId, customer.getCustomerId());
        if (lock) query.last("FOR UPDATE");
        BookingOrder order = bookingOrderMapper.selectOne(query);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        return order;
    }
}
