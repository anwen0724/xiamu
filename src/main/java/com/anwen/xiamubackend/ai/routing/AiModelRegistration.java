package com.anwen.xiamubackend.ai.routing;

import org.springframework.ai.chat.client.ChatClient;

/**
 * AI 模型注册信息。
 *
 * @param key 模型 key
 * @param provider 提供方
 * @param model 模型名
 * @param baseUrl 基础地址
 * @param chatClient 聊天客户端
 */
public record AiModelRegistration(
        String key,
        String provider,
        String model,
        String baseUrl,
        ChatClient chatClient) {
}
