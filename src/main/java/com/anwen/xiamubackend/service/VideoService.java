package com.anwen.xiamubackend.service;

import com.anwen.xiamubackend.model.entity.Video;
import com.anwen.xiamubackend.model.vo.VideoDashboardVO;
import com.anwen.xiamubackend.model.vo.VideoProcessStatusVO;
import com.anwen.xiamubackend.model.vo.VideoResultVO;
import com.anwen.xiamubackend.model.vo.VideoVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

/**
 * 视频 Service 接口
 */
public interface VideoService extends IService<Video> {

    /**
     * 上传视频
     * @param file 视频文件
     * @param userId 当前用户 ID
     * @return 视频信息
     */
    VideoVO uploadVideo(MultipartFile file, Long userId);

    VideoResultVO getVideoResult(Long videoId, Long userId);

    VideoProcessStatusVO getVideoStatus(Long videoId, Long userId);

    Page<VideoDashboardVO> listUserVideos(Long userId, long current, long pageSize, String status);

    boolean retryVideo(Long videoId, Long userId);
}
