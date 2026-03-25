package com.anwen.xiamubackend.service;

/**
 * 文件生命周期管理服务。
 */
public interface FileLifecycleService {

    void cleanupExpiredFiles();
}
