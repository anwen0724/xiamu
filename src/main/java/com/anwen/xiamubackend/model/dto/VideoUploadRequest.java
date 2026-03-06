package com.anwen.xiamubackend.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 视频上传请求参数
 */
@Data
public class VideoUploadRequest {

    /**
     * 上传的视频文件
     */
    @NotBlank(message = "文件不能为空")
    private MultipartFile file;
}
