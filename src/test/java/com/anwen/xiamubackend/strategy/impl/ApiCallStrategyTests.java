package com.anwen.xiamubackend.strategy.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiCallStrategyTests {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    @Test
    void transcribeShouldNormalizeVerboseJsonResponse() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/audio/transcriptions", exchange -> respond(exchange, 200, """
                {
                  "text": "接口转写文本",
                  "language": "zh",
                  "segments": [
                    { "id": 7, "start": 0.0, "end": 1.5, "text": "第一句" }
                  ]
                }
                """));
        server.start();

        ApiCallStrategy strategy = createStrategy("test-key", "http://localhost:" + server.getAddress().getPort(), "whisper-1");
        Path audioFile = Files.createTempFile("api-strategy", ".mp3");

        try {
            String result = strategy.transcribe(audioFile.toString());
            JsonNode jsonNode = objectMapper.readTree(result);

            assertEquals("接口转写文本", jsonNode.path("text").asText());
            assertEquals("zh", jsonNode.path("language").asText());
            assertEquals(1, jsonNode.path("segments").size());
            assertEquals("第一句", jsonNode.path("segments").get(0).path("text").asText());
        } finally {
            Files.deleteIfExists(audioFile);
        }
    }

    @Test
    void transcribeShouldFailWhenApiKeyMissing() throws Exception {
        ApiCallStrategy strategy = createStrategy("", "http://localhost:9999", "whisper-1");
        Path audioFile = Files.createTempFile("api-strategy-key", ".mp3");

        try {
            Exception exception = assertThrows(Exception.class, () -> strategy.transcribe(audioFile.toString()));
            assertTrue(exception.getMessage().contains("API Key"));
        } finally {
            Files.deleteIfExists(audioFile);
        }
    }

    @Test
    void transcribeShouldFailWhenRemoteReturnsError() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/audio/transcriptions", exchange -> respond(exchange, 401, """
                {"error":{"message":"invalid api key"}}
                """));
        server.start();

        ApiCallStrategy strategy = createStrategy("bad-key", "http://localhost:" + server.getAddress().getPort(), "whisper-1");
        Path audioFile = Files.createTempFile("api-strategy-error", ".mp3");

        try {
            Exception exception = assertThrows(Exception.class, () -> strategy.transcribe(audioFile.toString()));
            assertTrue(exception.getMessage().contains("状态码: 401"));
        } finally {
            Files.deleteIfExists(audioFile);
        }
    }

    private ApiCallStrategy createStrategy(String apiKey, String baseUrl, String model) {
        ApiCallStrategy strategy = new ApiCallStrategy(objectMapper);
        ReflectionTestUtils.setField(strategy, "apiKey", apiKey);
        ReflectionTestUtils.setField(strategy, "baseUrl", baseUrl);
        ReflectionTestUtils.setField(strategy, "model", model);
        ReflectionTestUtils.setField(strategy, "timeoutSeconds", 30);
        return strategy;
    }

    private void respond(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] responseBytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
        exchange.close();
    }
}
