package com.anwen.xiamubackend.strategy.impl;

import com.anwen.xiamubackend.strategy.TranscriptionStrategy;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.UUID;

/**
 * API 调用转录策略。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiCallStrategy implements TranscriptionStrategy {

    private static final String TRANSCRIPTION_PATH = "/v1/audio/transcriptions";

    private final ObjectMapper objectMapper;

    @Value("${xiamu.whisper.api-key:}")
    private String apiKey;

    @Value("${xiamu.whisper.base-url:https://api.openai.com}")
    private String baseUrl;

    @Value("${xiamu.whisper.api-model:whisper-1}")
    private String model;

    @Value("${xiamu.whisper.api-timeout-seconds:180}")
    private int timeoutSeconds;

    @Override
    public String transcribe(String audioPath) throws Exception {
        log.info("使用 API 模式转写音频: {}", audioPath);

        File audioFile = new File(audioPath);
        if (!audioFile.exists()) {
            throw new Exception("音频文件不存在: " + audioPath);
        }
        if (!StringUtils.hasText(apiKey)) {
            throw new Exception("Whisper API Key 未配置，请检查 xiamu.whisper.api-key");
        }
        if (!StringUtils.hasText(baseUrl)) {
            throw new Exception("Whisper API Base URL 未配置，请检查 xiamu.whisper.base-url");
        }
        if (!StringUtils.hasText(model)) {
            throw new Exception("Whisper API 模型未配置，请检查 xiamu.whisper.api-model");
        }

        String boundary = "----XiamuBoundary" + UUID.randomUUID().toString().replace("-", "");
        byte[] requestBody = buildMultipartBody(audioFile, boundary);
        URI uri = URI.create(trimTrailingSlash(baseUrl) + TRANSCRIPTION_PATH);

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(Math.max(timeoutSeconds, 30)))
                .header("Authorization", "Bearer " + apiKey.trim())
                .header("Accept", "application/json")
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody))
                .build();

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.min(Math.max(timeoutSeconds, 30), 60)))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() >= 400) {
            throw new Exception("Whisper API 调用失败，状态码: " + response.statusCode() + "，响应: " + abbreviate(response.body()));
        }
        if (!StringUtils.hasText(response.body())) {
            throw new Exception("Whisper API 返回为空");
        }

        String normalizedResult = normalizeResponse(response.body());
        log.info("API 模式转写成功，JSON 长度: {}", normalizedResult.length());
        return normalizedResult;
    }

    private byte[] buildMultipartBody(File audioFile, String boundary) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeFormField(outputStream, boundary, "model", model.trim());
        writeFormField(outputStream, boundary, "response_format", "verbose_json");
        writeFormField(outputStream, boundary, "timestamp_granularities[]", "segment");
        writeFileField(outputStream, boundary, "file", audioFile);
        outputStream.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return outputStream.toByteArray();
    }

    private void writeFormField(ByteArrayOutputStream outputStream, String boundary, String name, String value) throws Exception {
        outputStream.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        outputStream.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        outputStream.write(value.getBytes(StandardCharsets.UTF_8));
        outputStream.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private void writeFileField(ByteArrayOutputStream outputStream, String boundary, String fieldName, File file) throws Exception {
        String contentType = Files.probeContentType(file.toPath());
        if (!StringUtils.hasText(contentType)) {
            contentType = "application/octet-stream";
        }
        outputStream.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        outputStream.write(("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + file.getName() + "\"\r\n")
                .getBytes(StandardCharsets.UTF_8));
        outputStream.write(("Content-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        outputStream.write(Files.readAllBytes(file.toPath()));
        outputStream.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private String normalizeResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        if (root.has("error")) {
            JsonNode errorNode = root.get("error");
            String message = errorNode.isObject() ? errorNode.path("message").asText("") : errorNode.asText("");
            throw new Exception("Whisper API 返回错误: " + (StringUtils.hasText(message) ? message : abbreviate(responseBody)));
        }

        String text = root.path("text").asText("").trim();
        if (!StringUtils.hasText(text)) {
            throw new Exception("Whisper API 返回的 text 为空");
        }

        ObjectNode normalized = objectMapper.createObjectNode();
        normalized.put("text", text);
        normalized.put("language", root.path("language").asText(""));
        normalized.put("language_probability", root.path("language_probability").asDouble(0.0));

        ArrayNode normalizedSegments = objectMapper.createArrayNode();
        JsonNode segments = root.path("segments");
        if (segments.isArray()) {
            for (int index = 0; index < segments.size(); index++) {
                JsonNode segment = segments.get(index);
                ObjectNode normalizedSegment = objectMapper.createObjectNode();
                normalizedSegment.put("id", segment.path("id").asInt(index));
                normalizedSegment.put("start", segment.path("start").asDouble(0.0));
                normalizedSegment.put("end", segment.path("end").asDouble(0.0));
                normalizedSegment.put("text", segment.path("text").asText(""));
                normalizedSegments.add(normalizedSegment);
            }
        }
        normalized.set("segments", normalizedSegments);
        return objectMapper.writeValueAsString(normalized);
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String abbreviate(String responseBody) {
        if (responseBody == null) {
            return "";
        }
        String sanitized = responseBody.replaceAll("\\s+", " ").trim();
        return sanitized.length() <= 300 ? sanitized : sanitized.substring(0, 300) + "...";
    }
}
