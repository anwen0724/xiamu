package com.anwen.xiamubackend.service.impl;

import com.anwen.xiamubackend.config.StorageConfig;
import com.anwen.xiamubackend.service.FileLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * 文件生命周期管理服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileLifecycleServiceImpl implements FileLifecycleService {

    private final StorageConfig storageConfig;

    @Override
    @Scheduled(cron = "${xiamu.storage.cleanup-cron:0 0 * * * ?}")
    public void cleanupExpiredFiles() {
        if (Boolean.FALSE.equals(storageConfig.getCleanupEnabled())) {
            return;
        }
        deleteExpiredFiles(storageConfig.getAudioPath(), Instant.now().minus(storageConfig.getAudioRetentionHours(), ChronoUnit.HOURS), "音频");
        deleteExpiredFiles(storageConfig.getVideoPath(), Instant.now().minus(storageConfig.getVideoRetentionDays(), ChronoUnit.DAYS), "视频");
    }

    private void deleteExpiredFiles(String rootPath, Instant expireBefore, String label) {
        if (rootPath == null || rootPath.isBlank()) {
            return;
        }
        Path root = Path.of(rootPath).toAbsolutePath();
        if (!Files.exists(root)) {
            return;
        }

        AtomicInteger deletedCount = new AtomicInteger();
        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> isExpired(path, expireBefore))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                            deletedCount.incrementAndGet();
                        } catch (IOException e) {
                            log.warn("{}文件删除失败: {}", label, path, e);
                        }
                    });
        } catch (IOException e) {
            log.warn("{}文件清理扫描失败: {}", label, root, e);
            return;
        }

        if (deletedCount.get() > 0) {
            log.info("{}文件清理完成，删除数量={}", label, deletedCount.get());
        }
    }

    private boolean isExpired(Path path, Instant expireBefore) {
        try {
            return Files.getLastModifiedTime(path).toInstant().isBefore(expireBefore);
        } catch (IOException e) {
            log.warn("读取文件修改时间失败: {}", path, e);
            return false;
        }
    }
}
