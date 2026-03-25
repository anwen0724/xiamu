package com.anwen.xiamubackend.service.impl;

import com.anwen.xiamubackend.exception.ErrorCode;
import com.anwen.xiamubackend.exception.ThrowUtils;
import com.anwen.xiamubackend.mapper.VideoMapper;
import com.anwen.xiamubackend.model.entity.Video;
import com.anwen.xiamubackend.model.enums.VideoProcessStageEnum;
import com.anwen.xiamubackend.model.enums.VideoStatusEnum;
import com.anwen.xiamubackend.model.enums.VideoTypeEnum;
import com.anwen.xiamubackend.model.vo.VideoChapterVO;
import com.anwen.xiamubackend.model.vo.VideoDashboardVO;
import com.anwen.xiamubackend.model.vo.VideoProcessStatusVO;
import com.anwen.xiamubackend.model.vo.VideoResultVO;
import com.anwen.xiamubackend.model.vo.VideoVO;
import com.anwen.xiamubackend.service.AudioExtractService;
import com.anwen.xiamubackend.service.VideoProcessStateService;
import com.anwen.xiamubackend.service.VideoService;
import com.anwen.xiamubackend.service.WhisperTranscriptionService;
import com.anwen.xiamubackend.utils.FFmpegUtil;
import com.anwen.xiamubackend.utils.FileStorageUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

/**
 * 视频 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoServiceImpl extends ServiceImpl<VideoMapper, Video> implements VideoService {

    private final FileStorageUtil fileStorageUtil;
    private final FFmpegUtil ffmpegUtil;
    private final AudioExtractService audioExtractService;
    private final WhisperTranscriptionService whisperTranscriptionService;
    private final VideoProcessStateService videoProcessStateService;
    private final VideoMapper videoMapper;
    private final ObjectMapper objectMapper;

    @Override
    public VideoVO uploadVideo(MultipartFile file, Long userId) {
        ThrowUtils.throwIf(userId == null, ErrorCode.NOT_LOGIN_ERROR, "未登录");

        // 1. 保存文件
        String filePath = fileStorageUtil.saveVideo(file);

        // 2. 获取视频信息
        FFmpegUtil.VideoInfo videoInfo = ffmpegUtil.getVideoInfo(filePath);

        // 3. 构建视频记录
        Video video = new Video();
        video.setUserId(userId);
        video.setTitle(file.getOriginalFilename());
        video.setUrl(filePath);
        video.setDuration(videoInfo.getDuration());
        video.setFileSize(file.getSize());
        video.setStatus(VideoStatusEnum.WAITING.getCode());
        video.setStage(VideoProcessStageEnum.QUEUED.getCode());
        video.setStageMessage(VideoProcessStageEnum.QUEUED.getDesc());
        video.setType(VideoTypeEnum.UPLOAD.getCode());

        // 获取文件扩展名作为格式
        String format = getFileExtension(file.getOriginalFilename());
        video.setFormat(format);

        // 4. 保存到数据库
        videoMapper.insert(video);

        log.info("视频上传成功: id={}, title={}, duration={}", video.getId(), video.getTitle(), video.getDuration());

        // 5. 异步提取音频
        audioExtractService.extractAudioAsync(video.getId());

        // 6. 返回结果
        return convertToVO(video);
    }

    @Override
    public VideoResultVO getVideoResult(Long videoId, Long userId) {
        Video video = getOwnedVideo(videoId, userId);
        VideoResultVO resultVO = new VideoResultVO();
        BeanUtils.copyProperties(video, resultVO);
        resultVO.setChapters(parseChapters(video.getChapters()));
        return resultVO;
    }

    @Override
    public VideoProcessStatusVO getVideoStatus(Long videoId, Long userId) {
        Video video = getOwnedVideo(videoId, userId);
        return videoProcessStateService.toStatusVO(video);
    }

    @Override
    public Page<VideoDashboardVO> listUserVideos(Long userId, long current, long pageSize, String status) {
        ThrowUtils.throwIf(userId == null, ErrorCode.NOT_LOGIN_ERROR, "未登录");
        ThrowUtils.throwIf(current <= 0 || pageSize <= 0, ErrorCode.PARAMS_ERROR, "分页参数非法");

        LambdaQueryWrapper<Video> queryWrapper = new LambdaQueryWrapper<Video>()
                .eq(Video::getUserId, userId)
                .orderByDesc(Video::getCreateTime);
        if (StringUtils.hasText(status)) {
            queryWrapper.eq(Video::getStatus, status.trim());
        }

        Page<Video> page = videoMapper.selectPage(new Page<>(current, pageSize), queryWrapper);
        Page<VideoDashboardVO> resultPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<VideoDashboardVO> records = page.getRecords().stream().map(video -> {
            VideoDashboardVO videoDashboardVO = new VideoDashboardVO();
            BeanUtils.copyProperties(video, videoDashboardVO);
            return videoDashboardVO;
        }).toList();
        resultPage.setRecords(records);
        return resultPage;
    }

    @Override
    public boolean retryVideo(Long videoId, Long userId) {
        Video video = getOwnedVideo(videoId, userId);
        ThrowUtils.throwIf(VideoStatusEnum.PROCESSING.getCode().equals(video.getStatus()), ErrorCode.OPERATION_ERROR, "当前视频正在处理中");
        ThrowUtils.throwIf(!StringUtils.hasText(video.getUrl()) && !StringUtils.hasText(video.getAudioPath()),
                ErrorCode.OPERATION_ERROR, "缺少可重试的源文件");

        videoProcessStateService.updateQueued(videoId, currentVideo -> {
            currentVideo.setTranscript(null);
            currentVideo.setSummary(null);
            currentVideo.setChapters(null);
        });

        if (StringUtils.hasText(video.getAudioPath())) {
            whisperTranscriptionService.transcribeAsync(video.getId(), video.getAudioPath());
        } else {
            audioExtractService.extractAudioAsync(video.getId());
        }
        return true;
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
     * 转换为 VO 对象
     */
    private VideoVO convertToVO(Video video) {
        VideoVO vo = new VideoVO();
        BeanUtils.copyProperties(video, vo);
        return vo;
    }

    private Video getOwnedVideo(Long videoId, Long userId) {
        ThrowUtils.throwIf(videoId == null || videoId <= 0, ErrorCode.PARAMS_ERROR, "视频 id 非法");
        ThrowUtils.throwIf(userId == null, ErrorCode.NOT_LOGIN_ERROR, "未登录");
        Video video = videoMapper.selectById(videoId);
        ThrowUtils.throwIf(video == null, ErrorCode.NOT_FOUND_ERROR, "视频不存在");
        ThrowUtils.throwIf(!userId.equals(video.getUserId()), ErrorCode.NO_AUTH_ERROR, "无权限访问该视频");
        return video;
    }

    private List<VideoChapterVO> parseChapters(String chapters) {
        if (!StringUtils.hasText(chapters)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(chapters, new TypeReference<>() {
            });
        } catch (Exception e) {
            log.warn("章节 JSON 解析失败，videoChapters={}", chapters, e);
            return Collections.emptyList();
        }
    }
}
