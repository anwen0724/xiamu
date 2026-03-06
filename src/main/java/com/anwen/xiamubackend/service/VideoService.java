package com.anwen.xiamubackend.service;

import com.anwen.xiamubackend.model.entity.Video;
import com.anwen.xiamubackend.model.vo.VideoVO;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

/**
 * 视频 Service 接口
 */
public interface VideoService extends IService<Video> {

    /**
     * 上传视频
     * @param file 视频文件
     * @return 视频信息
     */
    VideoVO uploadVideo(MultipartFile file);
}
