package com.univer.learnify.controller;

import com.univer.learnify.dto.request.CreateLessonRequest;
import com.univer.learnify.dto.request.CreateQuizRequest;
import com.univer.learnify.dto.response.LessonResponse;
import com.univer.learnify.dto.response.QuizResponse;
import com.univer.learnify.mapper.DtoMapper;
import com.univer.learnify.service.CourseService;
import com.univer.learnify.service.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/modules")
@RequiredArgsConstructor
public class ModuleController {

    private final CourseService courseService;
    private final QuizService quizService;

    @PostMapping("/{moduleId}/lessons")
    @ResponseStatus(HttpStatus.CREATED)
    public LessonResponse addLesson(@PathVariable Long moduleId, @Valid @RequestBody CreateLessonRequest request) {
        return DtoMapper.toLesson(courseService.addLesson(moduleId, request));
    }

    @DeleteMapping("/{moduleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long moduleId) {
        courseService.deleteModule(moduleId);
    }

    @PostMapping("/{moduleId}/quiz")
    @ResponseStatus(HttpStatus.CREATED)
    public QuizResponse createQuiz(@PathVariable Long moduleId, @Valid @RequestBody CreateQuizRequest request) {
        return DtoMapper.toQuiz(quizService.createQuiz(moduleId, request));
    }

    @GetMapping("/{moduleId}/quiz")
    public QuizResponse getQuiz(@PathVariable Long moduleId) {
        return DtoMapper.toQuiz(quizService.getQuizOfModule(moduleId));
    }
}
