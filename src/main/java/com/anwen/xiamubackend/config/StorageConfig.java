package com.anwen.xiamubackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文件存储配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "xiamu.storage")
public class StorageConfig {

    /**
     * 存储根目录
     */
    private String rootPath;

    /**
     * 视频文件存储路径
     */
    private String videoPath;

    /**
     * 最大文件大小
     */
    private String maxFileSize;

    /**
     * 允许的视频格式
     */
    private List<String> allowedFormats;
}
