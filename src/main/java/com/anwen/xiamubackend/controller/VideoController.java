package com.anwen.xiamubackend.controller;

import com.anwen.xiamubackend.common.BaseResponse;
import com.anwen.xiamubackend.common.ResultUtils;
import com.anwen.xiamubackend.exception.ErrorCode;
import com.anwen.xiamubackend.exception.ThrowUtils;
import com.anwen.xiamubackend.model.entity.User;
import com.anwen.xiamubackend.model.vo.VideoDashboardVO;
import com.anwen.xiamubackend.model.vo.VideoProcessStatusVO;
import com.anwen.xiamubackend.model.vo.VideoResultVO;
import com.anwen.xiamubackend.model.vo.VideoVO;
import com.anwen.xiamubackend.service.VideoProcessSseService;
import com.anwen.xiamubackend.service.UserService;
import com.anwen.xiamubackend.service.VideoService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
    private final VideoProcessSseService videoProcessSseService;
    private final UserService userService;

    @PostMapping("/upload")
    @Operation(summary = "上传视频")
    public BaseResponse<VideoVO> uploadVideo(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        ThrowUtils.throwIf(file == null || file.isEmpty(), ErrorCode.PARAMS_ERROR, "文件不能为空");
        User loginUser = userService.getLoginUser(request);
        VideoVO result = videoService.uploadVideo(file, loginUser.getId());
        return ResultUtils.success(result);
    }

    @GetMapping("/result/{videoId}")
    @Operation(summary = "获取视频结果详情")
    public BaseResponse<VideoResultVO> getVideoResult(@PathVariable Long videoId, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(videoService.getVideoResult(videoId, loginUser.getId()));
    }

    @GetMapping("/status/{videoId}")
    @Operation(summary = "获取视频处理状态")
    public BaseResponse<VideoProcessStatusVO> getVideoStatus(@PathVariable Long videoId, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(videoService.getVideoStatus(videoId, loginUser.getId()));
    }

    @GetMapping(path = "/status/stream/{videoId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "订阅视频处理状态流")
    public SseEmitter streamVideoStatus(@PathVariable Long videoId, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        VideoProcessStatusVO statusVO = videoService.getVideoStatus(videoId, loginUser.getId());
        return videoProcessSseService.subscribe(videoId, statusVO);
    }

    @GetMapping("/my/page")
    @Operation(summary = "分页获取当前用户视频列表")
    public BaseResponse<Page<VideoDashboardVO>> listMyVideos(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String status,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(videoService.listUserVideos(loginUser.getId(), current, pageSize, status));
    }

    @PostMapping("/retry/{videoId}")
    @Operation(summary = "重试视频处理")
    public BaseResponse<Boolean> retryVideo(@PathVariable Long videoId, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(videoService.retryVideo(videoId, loginUser.getId()));
    }
}
