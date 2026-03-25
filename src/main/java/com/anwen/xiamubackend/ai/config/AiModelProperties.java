package com.anwen.xiamubackend.ai.config;

import lombok.Data;

/**
 * 单个 AI 模型配置。
 */
@Data
public class AiModelProperties {

    private Boolean enabled = true;

    private String provider = "openai-compatible";

    private String apiKey;

    private String baseUrl;

    private String model;

    private Double temperature = 0.3D;
}
