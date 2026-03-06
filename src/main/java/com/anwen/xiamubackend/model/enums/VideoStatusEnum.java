package com.anwen.xiamubackend.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 视频处理状态枚举
 */
@Getter
@AllArgsConstructor
public enum VideoStatusEnum {

    /**
     * 等待处理
     */
    WAITING("WAITING", "等待处理"),

    /**
     * 处理中
     */
    PROCESSING("PROCESSING", "处理中"),

    /**
     * 处理完成
     */
    SUCCESS("SUCCESS", "处理完成"),

    /**
     * 处理失败
     */
    FAILED("FAILED", "处理失败");

    /**
     * 状态码
     */
    private final String code;

    /**
     * 状态描述
     */
    private final String desc;
}
