package com.anwen.xiamubackend.utils;

import com.anwen.xiamubackend.config.StorageConfig;
import com.anwen.xiamubackend.exception.BusinessException;
import com.anwen.xiamubackend.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;

/**
 * 文件存储工具类
 * 负责视频文件的校验、保存和管理
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileStorageUtil {

    private final StorageConfig storageConfig;

    /**
     * 保存视频文件
     * @param file 上传的文件
     * @return 保存后的文件路径
     */
    public String saveVideo(MultipartFile file) {
        // 校验文件
        validateFile(file);

        // 生成文件名
        String fileName = generateFileName(file.getOriginalFilename());

        // 生成存储路径（跨平台，使用绝对路径）
        Path relativePath = generateRelativePath();
        Path fullPath = Paths.get(storageConfig.getVideoPath(), relativePath.toString(), fileName).toAbsolutePath();

        // 确保目录存在
        File directory = fullPath.getParent().toFile();
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // 保存文件
        try {
            file.transferTo(fullPath.toFile());
            log.info("视频文件保存成功: {}", fullPath);
            return fullPath.toString();
        } catch (IOException e) {
            log.error("视频文件保存失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件保存失败");
        }
    }

    /**
     * 校验文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件不能为空");
        }

        // 校验文件大小
        long size = file.getSize();
        long maxSize = parseSize(storageConfig.getMaxFileSize());
        if (size > maxSize) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小超过限制");
        }

        // 校验文件格式
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件名不能为空");
        }

        String extension = getFileExtension(originalFilename);
        List<String> allowedFormats = storageConfig.getAllowedFormats();
        if (!allowedFormats.contains(extension.toLowerCase())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件格式不支持");
        }
    }

    /**
     * 生成文件名
     */
    private String generateFileName(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        return "video_" + System.currentTimeMillis() + "." + extension;
    }

    /**
     * 生成相对路径（按日期）
     * @return 日期路径对象
     */
    private Path generateRelativePath() {
        LocalDate now = LocalDate.now();
        return Paths.get(
            String.valueOf(now.getYear()),
            String.format("%02d", now.getMonthValue()),
            String.format("%02d", now.getDayOfMonth())
        );
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0) {
            return filename.substring(lastDot + 1);
        }
        return "";
    }

    /**
     * 解析文件大小
     * 支持 GB/MB/KB 单位
     */
    private long parseSize(String size) {
        if (size == null) {
            return 2L * 1024 * 1024 * 1024; // 默认 2GB
        }
        size = size.toUpperCase().trim();
        if (size.endsWith("GB")) {
            return Long.parseLong(size.substring(0, size.length() - 2)) * 1024 * 1024 * 1024;
        } else if (size.endsWith("MB")) {
            return Long.parseLong(size.substring(0, size.length() - 2)) * 1024 * 1024;
        } else if (size.endsWith("KB")) {
            return Long.parseLong(size.substring(0, size.length() - 2)) * 1024;
        } else {
            return Long.parseLong(size);
        }
    }
}
