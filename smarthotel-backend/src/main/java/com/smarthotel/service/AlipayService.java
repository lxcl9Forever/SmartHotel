package com.smarthotel.service;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.request.AlipayTradeFastpayRefundQueryRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.alipay.api.response.AlipayTradeFastpayRefundQueryResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthotel.config.AlipayConfig;
import com.smarthotel.entity.BookingOrder;
import com.smarthotel.entity.Customer;
import com.smarthotel.entity.Payment;
import com.smarthotel.entity.RefundRecord;
import com.smarthotel.entity.RoomInventory;
import com.smarthotel.entity.SysUser;
import com.smarthotel.exception.BusinessException;
import com.smarthotel.mapper.BookingOrderMapper;
import com.smarthotel.mapper.CustomerMapper;
import com.smarthotel.mapper.PaymentMapper;
import com.smarthotel.mapper.RoomInventoryMapper;
import com.smarthotel.mapper.SysUserMapper;
import com.smarthotel.vo.AlipayPageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlipayService {
    private final AlipayConfig config;
    private final ObjectMapper objectMapper;
    private final SysUserMapper sysUserMapper;
    private final CustomerMapper customerMapper;
    private final BookingOrderMapper bookingOrderMapper;
    private final RoomInventoryMapper roomInventoryMapper;
    private final PaymentMapper paymentMapper;
    private final DailyCheckinService dailyCheckinService;

    @Transactional
    public AlipayPageVO createPage(String username, Long orderId) {
        requireConfigured();
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        if (user == null) throw new BusinessException(401, "当前用户不存在");
        Customer customer = customerMapper.selectOne(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getUserId, user.getUserId()));
        if (customer == null) throw new BusinessException(403, "当前账号没有客户档案");

        BookingOrder order = bookingOrderMapper.selectOne(new LambdaQueryWrapper<BookingOrder>()
                .eq(BookingOrder::getOrderId, orderId)
                .eq(BookingOrder::getCustomerId, customer.getCustomerId())
                .last("FOR UPDATE"));
        if (order == null) throw new BusinessException(404, "订单不存在");
        if (!"WAIT_PAY".equals(order.getOrderStatus())
                || !"UNPAID".equals(order.getPaymentStatus())
                || order.getExpireTime() == null
                || !order.getExpireTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException(409, "订单已支付、取消或超过支付时间");
        }

        Payment payment = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOrderId, orderId)
                .eq(Payment::getPaymentMethod, "ALIPAY")
                .eq(Payment::getPaymentStatus, "PENDING")
                .orderByDesc(Payment::getPaymentId)
                .last("LIMIT 1"));
        if (payment == null) {
            payment = new Payment();
            payment.setPaymentNo(UUID.randomUUID().toString().replace("-", "").toUpperCase());
            payment.setOrderId(orderId);
            payment.setPaymentMethod("ALIPAY");
            payment.setAmount(order.getTotalAmount());
            payment.setPaymentStatus("PENDING");
            paymentMapper.insert(payment);
        }

        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        if (StringUtils.hasText(config.getNotifyUrl())) {
            request.setNotifyUrl(config.getNotifyUrl());
        }
        try {
            request.setBizContent(objectMapper.writeValueAsString(Map.of(
                    "out_trade_no", payment.getPaymentNo(),
                    "total_amount", payment.getAmount().toPlainString(),
                    "subject", "SmartHotel订单" + order.getOrderNo(),
                    "product_code", "FAST_INSTANT_TRADE_PAY")));
            AlipayTradePagePayResponse response = client().pageExecute(request);
            if (!StringUtils.hasText(response.getBody())) {
                throw new BusinessException(502, "支付宝未返回支付页面");
            }
            return new AlipayPageVO(orderId, payment.getPaymentNo(), response.getBody());
        } catch (JsonProcessingException | AlipayApiException error) {
            log.error("Could not create sandbox payment page for order {}", orderId, error);
            throw new BusinessException(502, "创建支付宝支付页面失败");
        }
    }

    @Transactional
    public boolean handleNotify(Map<String, String> params) {
        requireConfigured();
        if (!verifySignature(params)) return false;
        if (!config.getAppId().equals(params.get("app_id"))) return false;
        if (StringUtils.hasText(config.getSellerId())
                && !config.getSellerId().equals(params.get("seller_id"))) return false;
        String tradeStatus = params.get("trade_status");
        if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) return false;
        if (!StringUtils.hasText(params.get("trade_no"))) return false;

        BigDecimal notifiedAmount;
        try {
            notifiedAmount = new BigDecimal(params.get("total_amount"));
        } catch (RuntimeException error) {
            return false;
        }
        return completeVerifiedPayment(params.get("out_trade_no"), params.get("trade_no"), notifiedAmount);
    }

    /** 本地沙箱联调无需公网回调；仅以支付宝服务端交易查询结果确认付款。 */
    @Transactional
    public String queryAndConfirm(String username, Long orderId) {
        requireConfigured();
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        if (user == null) throw new BusinessException(401, "当前用户不存在");
        Customer customer = customerMapper.selectOne(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getUserId, user.getUserId()));
        if (customer == null) throw new BusinessException(403, "当前账号没有客户档案");
        BookingOrder ownedOrder = bookingOrderMapper.selectOne(new LambdaQueryWrapper<BookingOrder>()
                .eq(BookingOrder::getOrderId, orderId)
                .eq(BookingOrder::getCustomerId, customer.getCustomerId()));
        if (ownedOrder == null) throw new BusinessException(404, "订单不存在");

        Payment payment = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOrderId, orderId)
                .eq(Payment::getPaymentMethod, "ALIPAY")
                .orderByDesc(Payment::getPaymentId)
                .last("LIMIT 1"));
        if (payment == null) throw new BusinessException(409, "请先发起支付宝沙箱支付");
        if ("SUCCESS".equals(payment.getPaymentStatus()) && "PAID".equals(ownedOrder.getPaymentStatus())) {
            return "PAID";
        }
        if ("FAILED".equals(payment.getPaymentStatus())) return "CLOSED";

        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        try {
            request.setBizContent(objectMapper.writeValueAsString(Map.of(
                    "out_trade_no", payment.getPaymentNo())));
            AlipayTradeQueryResponse response = client().execute(request);
            if (!response.isSuccess()) {
                if ("ACQ.TRADE_NOT_EXIST".equals(response.getSubCode())) return "PENDING";
                log.warn("Alipay query failed for order {}: {}", orderId, response.getSubCode());
                throw new BusinessException(502, "支付宝交易查询失败，请稍后重试");
            }
            if (!payment.getPaymentNo().equals(response.getOutTradeNo())) {
                throw new BusinessException(502, "支付宝返回的交易订单号不匹配");
            }
            String tradeStatus = response.getTradeStatus();
            if ("WAIT_BUYER_PAY".equals(tradeStatus)) return "PENDING";
            if ("TRADE_CLOSED".equals(tradeStatus)) {
                paymentMapper.update(null, new LambdaUpdateWrapper<Payment>()
                        .eq(Payment::getPaymentId, payment.getPaymentId())
                        .eq(Payment::getPaymentStatus, "PENDING")
                        .set(Payment::getPaymentStatus, "FAILED"));
                return "CLOSED";
            }
            if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
                return "PENDING";
            }
            if (!StringUtils.hasText(response.getTradeNo())) {
                throw new BusinessException(502, "支付宝未返回交易流水号");
            }
            BigDecimal amount;
            try {
                amount = new BigDecimal(response.getTotalAmount());
            } catch (RuntimeException error) {
                throw new BusinessException(502, "支付宝返回的交易金额无效");
            }
            if (!completeVerifiedPayment(payment.getPaymentNo(), response.getTradeNo(), amount)) {
                throw new BusinessException(409, "订单已取消或状态异常，请联系管理员核对支付记录");
            }
            return "PAID";
        } catch (JsonProcessingException | AlipayApiException error) {
            log.error("Could not query sandbox trade for order {}", orderId, error);
            if (error instanceof AlipayApiException && error.getMessage() != null
                    && error.getMessage().contains("当前配置的支付宝公钥为应用公钥")) {
                throw new BusinessException(502, "支付宝公钥配置错误：请将 ALIPAY_PUBLIC_KEY_FILE 指向沙箱应用的支付宝公钥文件");
            }
            throw new BusinessException(502, "支付宝交易查询失败，请稍后重试");
        }
    }

    /** Use one stable request number per refund so a retry cannot create a second refund. */
    public void refund(Payment payment, RefundRecord refund, String requestNo) {
        requireConfigured();
        if (payment == null || !"ALIPAY".equals(payment.getPaymentMethod())
                || !"SUCCESS".equals(payment.getPaymentStatus())
                || !StringUtils.hasText(payment.getPaymentNo())
                || !StringUtils.hasText(payment.getThirdPartyNo())
                || refund == null || refund.getRefundAmount() == null
                || refund.getRefundAmount().signum() <= 0
                || payment.getAmount() == null
                || payment.getAmount().compareTo(refund.getRefundAmount()) < 0
                || !StringUtils.hasText(requestNo)) {
            throw new BusinessException(409, "支付宝支付记录与退款金额不匹配，请核对后处理");
        }

        AlipayClient apiClient = client();
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        try {
            request.setBizContent(objectMapper.writeValueAsString(Map.of(
                    "out_trade_no", payment.getPaymentNo(),
                    "trade_no", payment.getThirdPartyNo(),
                    "refund_amount", refund.getRefundAmount().toPlainString(),
                    "refund_reason", refund.getRefundReason() == null ? "订单退款" : refund.getRefundReason(),
                    "out_request_no", requestNo)));
        } catch (JsonProcessingException error) {
            throw new BusinessException(500, "构造支付宝退款请求失败");
        }

        try {
            AlipayTradeRefundResponse response = apiClient.execute(request);
            if (response.isSuccess()
                    && "Y".equals(response.getFundChange())
                    && payment.getPaymentNo().equals(response.getOutTradeNo())
                    && payment.getThirdPartyNo().equals(response.getTradeNo())
                    && sameAmount(refund.getRefundAmount(), response.getRefundFee())) {
                return;
            }
            // A replay may return fund_change=N. Confirm the original request before recording success.
            if (queryRefundSucceeded(apiClient, payment, refund.getRefundAmount(), requestNo)) {
                return;
            }
            log.warn("Alipay refund not confirmed for refund {}: {}", refund.getRefundId(), response.getSubCode());
            throw new BusinessException(502, "支付宝退款未成功，请核对沙箱交易后重试该申请");
        } catch (AlipayApiException error) {
            // A timeout can happen after Alipay has refunded. Query with the SAME request number.
            log.warn("Alipay refund response unavailable for refund {}", refund.getRefundId(), error);
            if (queryRefundSucceeded(apiClient, payment, refund.getRefundAmount(), requestNo)) {
                return;
            }
            throw new BusinessException(502, "支付宝退款结果未确认，请稍后重试该申请");
        }
    }

    private boolean queryRefundSucceeded(AlipayClient apiClient, Payment payment,
                                         BigDecimal amount, String requestNo) {
        AlipayTradeFastpayRefundQueryRequest request = new AlipayTradeFastpayRefundQueryRequest();
        try {
            request.setBizContent(objectMapper.writeValueAsString(Map.of(
                    "out_trade_no", payment.getPaymentNo(),
                    "trade_no", payment.getThirdPartyNo(),
                    "out_request_no", requestNo)));
            AlipayTradeFastpayRefundQueryResponse response = apiClient.execute(request);
            if (!response.isSuccess()) {
                log.warn("Alipay refund query failed for request {}: {}", requestNo, response.getSubCode());
                throw new BusinessException(502, "支付宝退款状态无法核实，请稍后重试该申请");
            }
            if (!StringUtils.hasText(response.getOutRequestNo())
                    && !StringUtils.hasText(response.getRefundAmount())) {
                return false;
            }
            if (requestNo.equals(response.getOutRequestNo())
                    && payment.getPaymentNo().equals(response.getOutTradeNo())
                    && payment.getThirdPartyNo().equals(response.getTradeNo())
                    && sameAmount(amount, response.getRefundAmount())
                    && (!StringUtils.hasText(response.getRefundStatus())
                        || "REFUND_SUCCESS".equals(response.getRefundStatus())
                        || "SUCCESS".equals(response.getRefundStatus()))) {
                return true;
            }
            throw new BusinessException(502, "支付宝退款记录与本地申请不一致，请人工核对");
        } catch (JsonProcessingException | AlipayApiException error) {
            log.warn("Could not query Alipay refund request {}", requestNo, error);
            throw new BusinessException(502, "支付宝退款状态无法核实，请稍后重试该申请");
        }
    }

    private boolean sameAmount(BigDecimal expected, String actual) {
        try {
            return actual != null && expected.compareTo(new BigDecimal(actual)) == 0;
        } catch (NumberFormatException error) {
            return false;
        }
    }

    private boolean completeVerifiedPayment(String paymentNo, String tradeNo, BigDecimal notifiedAmount) {
        Payment found = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getPaymentNo, paymentNo)
                .eq(Payment::getPaymentMethod, "ALIPAY"));
        if (found == null) return false;
        // Keep the same lock order as checkout and timeout cancellation: order, then payment.
        BookingOrder order = bookingOrderMapper.selectOne(new LambdaQueryWrapper<BookingOrder>()
                .eq(BookingOrder::getOrderId, found.getOrderId()).last("FOR UPDATE"));
        if (order == null) return false;
        Payment payment = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getPaymentId, found.getPaymentId()).last("FOR UPDATE"));
        if (payment == null) return false;
        if (payment.getAmount().compareTo(notifiedAmount) != 0) return false;
        if ("SUCCESS".equals(payment.getPaymentStatus())) {
            return tradeNo.equals(payment.getThirdPartyNo());
        }
        if (!"PENDING".equals(payment.getPaymentStatus())) return false;

        if (!"WAIT_PAY".equals(order.getOrderStatus())
                || !"UNPAID".equals(order.getPaymentStatus())
                || payment.getAmount().compareTo(order.getTotalAmount()) != 0) {
            log.error("Paid Alipay trade {} needs manual reconciliation for order {}",
                    tradeNo, payment.getOrderId());
            return false;
        }

        List<RoomInventory> inventory = roomInventoryMapper.selectList(
                new LambdaQueryWrapper<RoomInventory>()
                        .eq(RoomInventory::getRoomTypeId, order.getRoomTypeId())
                        .ge(RoomInventory::getInventoryDate, order.getCheckInDate())
                        .lt(RoomInventory::getInventoryDate, order.getCheckOutDate())
                        .orderByAsc(RoomInventory::getInventoryDate)
                        .last("FOR UPDATE"));
        if (inventory.size() != order.getNights()) {
            throw new IllegalStateException("支付回调订单库存日期不完整: " + order.getOrderId());
        }
        for (RoomInventory day : inventory) {
            if (day.getLockedQty() < order.getRoomCount()) {
                throw new IllegalStateException("支付回调订单库存锁定数量异常: " + order.getOrderId());
            }
            day.setLockedQty(day.getLockedQty() - order.getRoomCount());
            day.setSoldQty(day.getSoldQty() + order.getRoomCount());
            roomInventoryMapper.updateById(day);
        }
        payment.setPaymentStatus("SUCCESS");
        payment.setThirdPartyNo(tradeNo);
        payment.setPaidTime(LocalDateTime.now());
        paymentMapper.updateById(payment);
        order.setPaymentStatus("PAID");
        order.setOrderStatus("CONFIRMED");
        bookingOrderMapper.updateById(order);
        dailyCheckinService.markUsed(order.getOrderId());
        return true;
    }

    private boolean verifySignature(Map<String, String> params) {
        try {
            return AlipaySignature.rsaCheckV1(params, readKey(config.getPublicKeyFile(), "支付宝公钥"),
                    "UTF-8", "RSA2");
        } catch (AlipayApiException error) {
            log.warn("Invalid Alipay callback signature", error);
            return false;
        }
    }

    AlipayClient client() {
        return new DefaultAlipayClient(config.getGateway(), config.getAppId(),
                readKey(config.getPrivateKeyFile(), "应用私钥"), "json", "UTF-8",
                readKey(config.getPublicKeyFile(), "支付宝公钥"), "RSA2");
    }

    private String readKey(String filename, String keyName) {
        try {
            return Files.readString(Path.of(filename), StandardCharsets.UTF_8)
                    .replaceAll("-----BEGIN [^-]+-----", "")
                    .replaceAll("-----END [^-]+-----", "")
                    .replaceAll("\\s", "");
        } catch (IOException | java.nio.file.InvalidPathException error) {
            throw new BusinessException(503, keyName + "文件无法读取：请检查 IDEA 环境变量中的文件完整路径");
        }
    }

    private void requireConfigured() {
        if (!config.isEnabled()) throw new BusinessException(503, "支付宝沙箱尚未启用");
        if (!StringUtils.hasText(config.getAppId())
                || !StringUtils.hasText(config.getPrivateKeyFile())
                || !StringUtils.hasText(config.getPublicKeyFile())) {
            throw new BusinessException(503, "支付宝沙箱配置不完整");
        }
        String host;
        try {
            host = URI.create(config.getGateway()).getHost();
        } catch (RuntimeException error) {
            throw new BusinessException(503, "支付宝沙箱网关地址无效");
        }
        if (host == null || !host.endsWith(".alipaydev.com")) {
            throw new BusinessException(503, "仅允许支付宝沙箱网关");
        }
    }
}
