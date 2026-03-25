package com.anwen.xiamubackend.service;

import com.anwen.xiamubackend.model.entity.Note;
import com.anwen.xiamubackend.model.vo.NoteVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 笔记服务。
 */
public interface NoteService extends IService<Note> {

    Long addNote(Note note, Long userId);

    boolean updateNote(Note note, Long userId);

    boolean deleteNote(Long noteId, Long userId);

    List<NoteVO> listNotesByVideoId(Long videoId, Long userId);
}
