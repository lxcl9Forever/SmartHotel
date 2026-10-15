package com.smarthotel.service;

import com.smarthotel.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DailyCheckinService {
    private static final ZoneId HOTEL_ZONE = ZoneId.of("Asia/Shanghai");
    private final JdbcTemplate jdbc;

    public record Reward(int dayNumber, BigDecimal amount, long claimCount) {}
    public record Coupon(long id, BigDecimal amount, LocalDateTime expireTime) {}
    public record Overview(int streakDays, boolean checkedInToday, int nextDay,
                           List<Reward> rewards, List<Coupon> coupons) {}

    public Overview overview(String username) {
        long customerId = customerId(username, false);
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        List<State> states = jdbc.query("SELECT last_checkin_date, streak_days FROM customer_checkin_state WHERE customer_id=?",
                (rs, row) -> new State(rs.getDate(1).toLocalDate(), rs.getInt(2)), customerId);
        State state = states.isEmpty() ? null : states.get(0);
        boolean checkedToday = state != null && today.equals(state.date());
        int streak = state == null || state.date().isBefore(today.minusDays(1)) ? 0 : state.streak();
        int nextDay = streak % 7 + 1;
        List<Coupon> coupons = jdbc.query("SELECT id, amount, expire_time FROM customer_coupon "
                        + "WHERE customer_id=? AND status='AVAILABLE' AND expire_time>? ORDER BY expire_time, id",
                (rs, row) -> new Coupon(rs.getLong(1), rs.getBigDecimal(2), rs.getTimestamp(3).toLocalDateTime()),
                customerId, Timestamp.valueOf(LocalDateTime.now(HOTEL_ZONE)));
        return new Overview(streak, checkedToday, nextDay, rewards(), coupons);
    }

    @Transactional
    public Overview checkIn(String username) {
        long customerId = customerId(username, true);
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        List<State> states = jdbc.query("SELECT last_checkin_date, streak_days FROM customer_checkin_state "
                        + "WHERE customer_id=? FOR UPDATE",
                (rs, row) -> new State(rs.getDate(1).toLocalDate(), rs.getInt(2)), customerId);
        State previous = states.isEmpty() ? null : states.get(0);
        if (previous != null && today.equals(previous.date())) {
            throw new BusinessException(409, "今天已经签到，请明天再来");
        }
        int day = previous != null && today.minusDays(1).equals(previous.date())
                ? previous.streak() % 7 + 1 : 1;
        List<BigDecimal> amounts = jdbc.query("SELECT amount FROM daily_checkin_reward WHERE day_number=?",
                (rs, row) -> rs.getBigDecimal(1), day);
        if (amounts.isEmpty()) throw new BusinessException(503, "签到奖励尚未配置");
        BigDecimal amount = amounts.get(0);
        LocalDateTime expires = CouponValidity.expiresAt("CHECKIN", LocalDateTime.now(HOTEL_ZONE));
        GeneratedKeyHolder holder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO customer_coupon (customer_id, amount, source, status, expire_time) VALUES (?, ?, 'CHECKIN', 'AVAILABLE', ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, customerId);
            statement.setBigDecimal(2, amount);
            statement.setTimestamp(3, Timestamp.valueOf(expires));
            return statement;
        }, holder);
        long couponId = holder.getKey().longValue();
        jdbc.update("INSERT INTO customer_checkin_record (customer_id, checkin_date, day_number, amount, coupon_id) "
                + "VALUES (?, ?, ?, ?, ?)", customerId, Date.valueOf(today), day, amount, couponId);
        if (previous == null) {
            jdbc.update("INSERT INTO customer_checkin_state (customer_id, last_checkin_date, streak_days) VALUES (?, ?, ?)",
                    customerId, Date.valueOf(today), day);
        } else {
            jdbc.update("UPDATE customer_checkin_state SET last_checkin_date=?, streak_days=? WHERE customer_id=?",
                    Date.valueOf(today), day, customerId);
        }
        return overview(username);
    }

    public List<Reward> rewards() {
        return jdbc.query("SELECT r.day_number, r.amount, COUNT(c.id) AS claim_count "
                        + "FROM daily_checkin_reward r LEFT JOIN customer_checkin_record c ON c.day_number=r.day_number "
                        + "GROUP BY r.day_number, r.amount ORDER BY r.day_number",
                (rs, row) -> new Reward(rs.getInt(1), rs.getBigDecimal(2), rs.getLong(3)));
    }

    @Transactional
    public void updateReward(int day, BigDecimal amount) {
        if (day < 1 || day > 7 || amount == null || amount.signum() <= 0
                || amount.scale() > 2 || amount.compareTo(new BigDecimal("10000")) > 0) {
            throw new BusinessException(400, "签到天数须为1至7，金额须大于0且不超过10000元，最多两位小数");
        }
        if (jdbc.update("UPDATE daily_checkin_reward SET amount=? WHERE day_number=?", amount, day) != 1) {
            throw new BusinessException(404, "签到奖励不存在");
        }
    }

    /** Called while the booking transaction holds the selected coupon row lock. */
    public BigDecimal lockDiscount(long customerId, Long couponId, BigDecimal payableAfterPromotion) {
        if (couponId == null) return BigDecimal.ZERO;
        List<Coupon> matches = jdbc.query("SELECT id, amount, expire_time FROM customer_coupon "
                        + "WHERE id=? AND customer_id=? AND status='AVAILABLE' FOR UPDATE",
                (rs, row) -> new Coupon(rs.getLong(1), rs.getBigDecimal(2), rs.getTimestamp(3).toLocalDateTime()),
                couponId, customerId);
        if (matches.isEmpty() || !matches.get(0).expireTime().isAfter(LocalDateTime.now(HOTEL_ZONE))) {
            throw new BusinessException(409, "优惠券不存在、已使用或已过期");
        }
        return matches.get(0).amount().min(payableAfterPromotion);
    }

    public void reserve(long customerId, long couponId, long orderId) {
        if (jdbc.update("UPDATE customer_coupon SET status='RESERVED', order_id=? "
                + "WHERE id=? AND customer_id=? AND status='AVAILABLE'", orderId, couponId, customerId) != 1) {
            throw new BusinessException(409, "优惠券已被其他订单使用");
        }
    }

    public void release(long orderId) {
        jdbc.update("UPDATE customer_coupon SET status='AVAILABLE', order_id=NULL "
                + "WHERE order_id=? AND status='RESERVED' AND expire_time>?", orderId,
                Timestamp.valueOf(LocalDateTime.now(HOTEL_ZONE)));
        jdbc.update("UPDATE customer_coupon SET status='EXPIRED', order_id=NULL "
                + "WHERE order_id=? AND status='RESERVED' AND expire_time<=?", orderId,
                Timestamp.valueOf(LocalDateTime.now(HOTEL_ZONE)));
    }

    public void markUsed(long orderId) {
        jdbc.update("UPDATE customer_coupon SET status='USED' WHERE order_id=? AND status='RESERVED'", orderId);
    }

    private long customerId(String username, boolean lock) {
        List<Long> ids = jdbc.query("SELECT c.customer_id FROM customer c JOIN sys_user u ON u.user_id=c.user_id "
                        + "WHERE u.username=?" + (lock ? " FOR UPDATE" : ""),
                (rs, row) -> rs.getLong(1), username);
        if (ids.isEmpty()) throw new BusinessException(403, "当前账号没有客户档案");
        return ids.get(0);
    }

    private record State(LocalDate date, int streak) {}
}
