package com.smarthotel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthotel.exception.BusinessException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class LLMClientTest {
    private HttpServer server;
    private LLMClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        client = new LLMClient(new ObjectMapper());
        ReflectionTestUtils.setField(client, "enabled", true);
        ReflectionTestUtils.setField(client, "url", "http://127.0.0.1:" + server.getAddress().getPort() + "/chat/completions");
        ReflectionTestUtils.setField(client, "model", "test-model");
        ReflectionTestUtils.setField(client, "apiKey", "test-key");
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void parsesModelResponseAndKeepsPoliteGreeting() {
        server.createContext("/chat/completions", exchange -> {
            String body = "{\"choices\":[{\"message\":{\"content\":\"{\\\"people\\\":3,\\\"days\\\":2,\\\"budget\\\":900,\\\"preference\\\":\\\"安静\\\",\\\"greeting\\\":\\\"您好，很高兴为您挑选合适的房型。\\\"}\"}}]}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });

        var result = client.parse("一家三口住两晚，预算900，希望安静");
        assertEquals(3, result.preference().people());
        assertEquals(2, result.preference().days());
        assertEquals("LLM", result.preference().source());
        assertEquals("您好，很高兴为您挑选合适的房型。", result.greeting());
    }

    @Test
    void failsInsteadOfUsingLocalParserWhenModelIsUnavailable() {
        server.createContext("/chat/completions", exchange -> {
            exchange.sendResponseHeaders(503, -1);
            exchange.close();
        });
        assertThrows(BusinessException.class, () -> client.parse("两人住一晚"));
    }
}
