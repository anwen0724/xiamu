package com.anwen.xiamubackend.controller;

import com.anwen.xiamubackend.common.BaseResponse;
import com.anwen.xiamubackend.common.ResultUtils;
import com.anwen.xiamubackend.exception.ErrorCode;
import com.anwen.xiamubackend.exception.ThrowUtils;
import com.anwen.xiamubackend.model.vo.VideoVO;
import com.anwen.xiamubackend.service.VideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 视频管理 Controller
 */
@Slf4j
@RestController
@RequestMapping("/video")
@RequiredArgsConstructor
@Tag(name = "视频管理", description = "视频上传、处理相关接口")
public class VideoController {

    private final VideoService videoService;

    @PostMapping("/upload")
    @Operation(summary = "上传视频")
    public BaseResponse<VideoVO> uploadVideo(@RequestParam("file") MultipartFile file) {
        ThrowUtils.throwIf(file == null || file.isEmpty(), ErrorCode.PARAMS_ERROR, "文件不能为空");
        VideoVO result = videoService.uploadVideo(file);
        return ResultUtils.success(result);
    }
}
