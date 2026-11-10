package com.smarthotel.service;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeFastpayRefundQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeFastpayRefundQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthotel.config.AlipayConfig;
import com.smarthotel.entity.Payment;
import com.smarthotel.entity.RefundRecord;
import com.smarthotel.exception.BusinessException;
import com.smarthotel.mapper.BookingOrderMapper;
import com.smarthotel.mapper.CustomerMapper;
import com.smarthotel.mapper.PaymentMapper;
import com.smarthotel.mapper.RoomInventoryMapper;
import com.smarthotel.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AlipayRefundServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AlipayClient client = mock(AlipayClient.class);
    private AlipayService service;

    @BeforeEach
    void setUp() {
        AlipayConfig config = new AlipayConfig();
        config.setEnabled(true);
        config.setGateway("https://openapi-sandbox.dl.alipaydev.com/gateway.do");
        config.setAppId("sandbox-app");
        config.setPrivateKeyFile("unused-private-key");
        config.setPublicKeyFile("unused-public-key");
        service = spy(new AlipayService(config, objectMapper,
                mock(SysUserMapper.class), mock(CustomerMapper.class),
                mock(BookingOrderMapper.class), mock(RoomInventoryMapper.class),
                mock(PaymentMapper.class), mock(DailyCheckinService.class)));
        doReturn(client).when(service).client();
    }

    @Test
    void successfulRefundUsesOriginalPaymentAndStableRequestNumber() throws Exception {
        AlipayTradeRefundResponse response = new AlipayTradeRefundResponse();
        response.setCode("10000");
        response.setFundChange("Y");
        response.setOutTradeNo("merchant-payment-30");
        response.setTradeNo("alipay-trade-30");
        response.setRefundFee("1588.00");
        when(client.execute(any(AlipayTradeRefundRequest.class))).thenReturn(response);

        service.refund(payment(), refund(), "RF000000000000000000000000000005");

        ArgumentCaptor<AlipayTradeRefundRequest> captured = ArgumentCaptor.forClass(AlipayTradeRefundRequest.class);
        verify(client).execute(captured.capture());
        JsonNode body = objectMapper.readTree(captured.getValue().getBizContent());
        assertEquals("merchant-payment-30", body.get("out_trade_no").asText());
        assertEquals("alipay-trade-30", body.get("trade_no").asText());
        assertEquals("1588.00", body.get("refund_amount").asText());
        assertEquals("RF000000000000000000000000000005", body.get("out_request_no").asText());
        verify(client, never()).execute(any(AlipayTradeFastpayRefundQueryRequest.class));
    }

    @Test
    void timeoutCanBeReconciledWithSameRequestNumber() throws Exception {
        when(client.execute(any(AlipayTradeRefundRequest.class)))
                .thenThrow(new AlipayApiException("timeout"));
        AlipayTradeFastpayRefundQueryResponse query = new AlipayTradeFastpayRefundQueryResponse();
        query.setCode("10000");
        query.setOutRequestNo("RF000000000000000000000000000005");
        query.setOutTradeNo("merchant-payment-30");
        query.setTradeNo("alipay-trade-30");
        query.setRefundAmount("1588.00");
        when(client.execute(any(AlipayTradeFastpayRefundQueryRequest.class))).thenReturn(query);

        service.refund(payment(), refund(), "RF000000000000000000000000000005");

        verify(client).execute(any(AlipayTradeFastpayRefundQueryRequest.class));
    }

    @Test
    void mismatchedRefundAmountIsNotRecordedAsSuccess() throws Exception {
        AlipayTradeRefundResponse response = new AlipayTradeRefundResponse();
        response.setCode("10000");
        response.setFundChange("N");
        when(client.execute(any(AlipayTradeRefundRequest.class))).thenReturn(response);
        AlipayTradeFastpayRefundQueryResponse query = new AlipayTradeFastpayRefundQueryResponse();
        query.setCode("10000");
        query.setOutRequestNo("RF000000000000000000000000000005");
        query.setOutTradeNo("merchant-payment-30");
        query.setTradeNo("alipay-trade-30");
        query.setRefundAmount("1.00");
        when(client.execute(any(AlipayTradeFastpayRefundQueryRequest.class))).thenReturn(query);

        assertThrows(BusinessException.class,
                () -> service.refund(payment(), refund(), "RF000000000000000000000000000005"));
    }

    private Payment payment() {
        Payment payment = new Payment();
        payment.setOrderId(30L);
        payment.setPaymentNo("merchant-payment-30");
        payment.setThirdPartyNo("alipay-trade-30");
        payment.setPaymentMethod("ALIPAY");
        payment.setPaymentStatus("SUCCESS");
        payment.setAmount(new BigDecimal("1588.00"));
        return payment;
    }

    private RefundRecord refund() {
        RefundRecord refund = new RefundRecord();
        refund.setRefundId(5L);
        refund.setRefundAmount(new BigDecimal("1588.00"));
        refund.setRefundReason("用户取消订单");
        return refund;
    }
}
