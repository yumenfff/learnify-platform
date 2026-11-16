package com.univer.learnify.controller;

import com.univer.learnify.dto.request.TakeQuizRequest;
import com.univer.learnify.dto.response.QuizResponse;
import com.univer.learnify.dto.response.QuizResultResponse;
import com.univer.learnify.mapper.DtoMapper;
import com.univer.learnify.service.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @GetMapping("/{id}")
    public QuizResponse get(@PathVariable Long id) {
        return DtoMapper.toQuiz(quizService.getQuiz(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        quizService.deleteQuiz(id);
    }

    @PostMapping("/{id}/take")
    @ResponseStatus(HttpStatus.CREATED)
    public QuizResultResponse take(@PathVariable Long id, @Valid @RequestBody TakeQuizRequest request) {
        return DtoMapper.toQuizResult(quizService.takeQuiz(id, request));
    }


    @GetMapping("/{id}/results")
    public List<QuizResultResponse> results(@PathVariable Long id) {
        return quizService.getResultsOfQuiz(id).stream().map(DtoMapper::toQuizResult).toList();
    }
}
