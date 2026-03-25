package com.anwen.xiamubackend.controller;

import com.anwen.xiamubackend.common.BaseResponse;
import com.anwen.xiamubackend.common.DeleteRequest;
import com.anwen.xiamubackend.common.ResultUtils;
import com.anwen.xiamubackend.exception.ErrorCode;
import com.anwen.xiamubackend.exception.ThrowUtils;
import com.anwen.xiamubackend.model.dto.note.NoteAddRequest;
import com.anwen.xiamubackend.model.dto.note.NoteUpdateRequest;
import com.anwen.xiamubackend.model.entity.Note;
import com.anwen.xiamubackend.model.entity.User;
import com.anwen.xiamubackend.model.vo.NoteVO;
import com.anwen.xiamubackend.service.NoteService;
import com.anwen.xiamubackend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 笔记接口。
 */
@RestController
@RequestMapping("/note")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;
    private final UserService userService;

    @PostMapping("/add")
    public BaseResponse<Long> addNote(@RequestBody NoteAddRequest noteAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(noteAddRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        User loginUser = userService.getLoginUser(request);
        Note note = new Note();
        BeanUtils.copyProperties(noteAddRequest, note);
        return ResultUtils.success(noteService.addNote(note, loginUser.getId()));
    }

    @PostMapping("/update")
    public BaseResponse<Boolean> updateNote(@RequestBody NoteUpdateRequest noteUpdateRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(noteUpdateRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        User loginUser = userService.getLoginUser(request);
        Note note = new Note();
        BeanUtils.copyProperties(noteUpdateRequest, note);
        return ResultUtils.success(noteService.updateNote(note, loginUser.getId()));
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteNote(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(noteService.deleteNote(deleteRequest.getId(), loginUser.getId()));
    }

    @GetMapping("/list/video")
    public BaseResponse<List<NoteVO>> listNotesByVideo(@RequestParam Long videoId, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(noteService.listNotesByVideoId(videoId, loginUser.getId()));
    }
}
