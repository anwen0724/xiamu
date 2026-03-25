package com.anwen.xiamubackend.service.impl;

import com.anwen.xiamubackend.exception.ErrorCode;
import com.anwen.xiamubackend.exception.ThrowUtils;
import com.anwen.xiamubackend.mapper.NoteMapper;
import com.anwen.xiamubackend.mapper.VideoMapper;
import com.anwen.xiamubackend.model.entity.Note;
import com.anwen.xiamubackend.model.entity.Video;
import com.anwen.xiamubackend.model.vo.NoteVO;
import com.anwen.xiamubackend.service.NoteService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 笔记服务实现。
 */
@Service
@RequiredArgsConstructor
public class NoteServiceImpl extends ServiceImpl<NoteMapper, Note> implements NoteService {

    private final NoteMapper noteMapper;
    private final VideoMapper videoMapper;

    @Override
    public Long addNote(Note note, Long userId) {
        ThrowUtils.throwIf(note == null, ErrorCode.PARAMS_ERROR, "笔记参数不能为空");
        ThrowUtils.throwIf(userId == null, ErrorCode.NOT_LOGIN_ERROR, "未登录");
        validateNote(note.getTitle(), note.getContent(), note.getTimeSeconds());
        checkVideoOwnership(note.getVideoId(), userId);

        note.setUserId(userId);
        int result = noteMapper.insert(note);
        ThrowUtils.throwIf(result <= 0, ErrorCode.OPERATION_ERROR, "新增笔记失败");
        return note.getId();
    }

    @Override
    public boolean updateNote(Note note, Long userId) {
        ThrowUtils.throwIf(note == null || note.getId() == null, ErrorCode.PARAMS_ERROR, "笔记参数错误");
        ThrowUtils.throwIf(userId == null, ErrorCode.NOT_LOGIN_ERROR, "未登录");
        Note oldNote = noteMapper.selectById(note.getId());
        ThrowUtils.throwIf(oldNote == null, ErrorCode.NOT_FOUND_ERROR, "笔记不存在");
        ThrowUtils.throwIf(!userId.equals(oldNote.getUserId()), ErrorCode.NO_AUTH_ERROR, "无权限修改该笔记");
        validateNote(note.getTitle(), note.getContent(), note.getTimeSeconds());

        Note updateNote = new Note();
        updateNote.setId(note.getId());
        updateNote.setTitle(note.getTitle().trim());
        updateNote.setContent(note.getContent().trim());
        updateNote.setTimeSeconds(note.getTimeSeconds());
        return noteMapper.updateById(updateNote) > 0;
    }

    @Override
    public boolean deleteNote(Long noteId, Long userId) {
        ThrowUtils.throwIf(noteId == null || noteId <= 0, ErrorCode.PARAMS_ERROR, "笔记 id 非法");
        ThrowUtils.throwIf(userId == null, ErrorCode.NOT_LOGIN_ERROR, "未登录");
        Note note = noteMapper.selectById(noteId);
        ThrowUtils.throwIf(note == null, ErrorCode.NOT_FOUND_ERROR, "笔记不存在");
        ThrowUtils.throwIf(!userId.equals(note.getUserId()), ErrorCode.NO_AUTH_ERROR, "无权限删除该笔记");
        return noteMapper.deleteById(noteId) > 0;
    }

    @Override
    public List<NoteVO> listNotesByVideoId(Long videoId, Long userId) {
        ThrowUtils.throwIf(videoId == null || videoId <= 0, ErrorCode.PARAMS_ERROR, "视频 id 非法");
        ThrowUtils.throwIf(userId == null, ErrorCode.NOT_LOGIN_ERROR, "未登录");
        checkVideoOwnership(videoId, userId);

        LambdaQueryWrapper<Note> queryWrapper = new LambdaQueryWrapper<Note>()
                .eq(Note::getVideoId, videoId)
                .eq(Note::getUserId, userId)
                .orderByDesc(Note::getCreateTime);
        return noteMapper.selectList(queryWrapper).stream().map(this::toNoteVO).toList();
    }

    private void validateNote(String title, String content, Integer timeSeconds) {
        ThrowUtils.throwIf(!StringUtils.hasText(title), ErrorCode.PARAMS_ERROR, "笔记标题不能为空");
        ThrowUtils.throwIf(!StringUtils.hasText(content), ErrorCode.PARAMS_ERROR, "笔记内容不能为空");
        ThrowUtils.throwIf(title.trim().length() > 100, ErrorCode.PARAMS_ERROR, "笔记标题过长");
        ThrowUtils.throwIf(content.trim().length() > 10000, ErrorCode.PARAMS_ERROR, "笔记内容过长");
        ThrowUtils.throwIf(timeSeconds != null && timeSeconds < 0, ErrorCode.PARAMS_ERROR, "时间点不能小于 0");
    }

    private void checkVideoOwnership(Long videoId, Long userId) {
        ThrowUtils.throwIf(videoId == null || videoId <= 0, ErrorCode.PARAMS_ERROR, "视频 id 非法");
        Video video = videoMapper.selectById(videoId);
        ThrowUtils.throwIf(video == null, ErrorCode.NOT_FOUND_ERROR, "视频不存在");
        ThrowUtils.throwIf(!userId.equals(video.getUserId()), ErrorCode.NO_AUTH_ERROR, "无权限访问该视频");
    }

    private NoteVO toNoteVO(Note note) {
        NoteVO noteVO = new NoteVO();
        BeanUtils.copyProperties(note, noteVO);
        return noteVO;
    }
}
