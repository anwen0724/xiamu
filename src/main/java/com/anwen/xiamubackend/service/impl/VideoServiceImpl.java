package com.anwen.xiamubackend.service.impl;

import com.anwen.xiamubackend.model.enums.VideoStatusEnum;
import com.anwen.xiamubackend.model.enums.VideoTypeEnum;
import com.anwen.xiamubackend.mapper.VideoMapper;
import com.anwen.xiamubackend.model.entity.Video;
import com.anwen.xiamubackend.model.vo.VideoVO;
import com.anwen.xiamubackend.service.AudioExtractService;
import com.anwen.xiamubackend.service.VideoService;
import com.anwen.xiamubackend.utils.FFmpegUtil;
import com.anwen.xiamubackend.utils.FileStorageUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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

    @Override
    public VideoVO uploadVideo(MultipartFile file) {
        // 1. 保存文件
        String filePath = fileStorageUtil.saveVideo(file);

        // 2. 获取视频信息
        FFmpegUtil.VideoInfo videoInfo = ffmpegUtil.getVideoInfo(filePath);

        // 3. 构建视频记录
        Video video = new Video();
        video.setTitle(file.getOriginalFilename());
        video.setUrl(filePath);
        video.setDuration(videoInfo.getDuration());
        video.setFileSize(file.getSize());
        video.setStatus(VideoStatusEnum.WAITING.getCode());
        video.setType(VideoTypeEnum.UPLOAD.getCode());

        // 获取文件扩展名作为格式
        String format = getFileExtension(file.getOriginalFilename());
        video.setFormat(format);

        // 4. 保存到数据库
        this.save(video);

        log.info("视频上传成功: id={}, title={}, duration={}", video.getId(), video.getTitle(), video.getDuration());

        // 5. 异步提取音频
        audioExtractService.extractAudioAsync(video.getId());

        // 6. 返回结果
        return convertToVO(video);
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
}
