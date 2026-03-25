package com.anwen.xiamubackend.service.impl;

import com.anwen.xiamubackend.exception.BusinessException;
import com.anwen.xiamubackend.mapper.NoteMapper;
import com.anwen.xiamubackend.mapper.VideoMapper;
import com.anwen.xiamubackend.model.entity.Note;
import com.anwen.xiamubackend.model.entity.Video;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoteServiceImplTests {

    @Mock
    private NoteMapper noteMapper;

    @Mock
    private VideoMapper videoMapper;

    private NoteServiceImpl noteService;

    @BeforeEach
    void setUp() {
        noteService = new NoteServiceImpl(noteMapper, videoMapper);
    }

    @Test
    void addNoteShouldCheckVideoOwnership() {
        Video video = new Video();
        video.setId(1L);
        video.setUserId(9L);
        when(videoMapper.selectById(1L)).thenReturn(video);
        when(noteMapper.insert(any(Note.class))).thenAnswer(invocation -> {
            Note note = invocation.getArgument(0);
            note.setId(101L);
            return 1;
        });

        Note note = new Note();
        note.setVideoId(1L);
        note.setTitle("重点");
        note.setContent("记录内容");
        Long noteId = noteService.addNote(note, 9L);

        assertEquals(101L, noteId);
        verify(noteMapper).insert(any(Note.class));
    }

    @Test
    void addNoteShouldRejectForeignVideo() {
        Video video = new Video();
        video.setId(1L);
        video.setUserId(8L);
        when(videoMapper.selectById(1L)).thenReturn(video);

        Note note = new Note();
        note.setVideoId(1L);
        note.setTitle("重点");
        note.setContent("记录内容");

        assertThrows(BusinessException.class, () -> noteService.addNote(note, 9L));
    }
}
