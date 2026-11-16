package com.univer.learnify.controller;

import com.univer.learnify.dto.request.CreateUserRequest;
import com.univer.learnify.dto.request.UpdateUserRequest;
import com.univer.learnify.dto.response.CourseSummaryResponse;
import com.univer.learnify.dto.response.QuizResultResponse;
import com.univer.learnify.dto.response.SubmissionResponse;
import com.univer.learnify.dto.response.UserResponse;
import com.univer.learnify.entity.enums.Role;
import com.univer.learnify.mapper.DtoMapper;
import com.univer.learnify.service.AssignmentService;
import com.univer.learnify.service.EnrollmentService;
import com.univer.learnify.service.QuizService;
import com.univer.learnify.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final EnrollmentService enrollmentService;
    private final AssignmentService assignmentService;
    private final QuizService quizService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return DtoMapper.toUser(userService.createUser(request));
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) {
        return DtoMapper.toUser(userService.getUser(id));
    }

    @GetMapping
    public List<UserResponse> list(@RequestParam(required = false) Role role) {
        return userService.getUsers(role).stream().map(DtoMapper::toUser).toList();
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return DtoMapper.toUser(userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        userService.deleteUser(id);
    }


    @GetMapping("/{id}/courses")
    public List<CourseSummaryResponse> courses(@PathVariable Long id) {
        return enrollmentService.getCoursesOfStudent(id).stream().map(DtoMapper::toCourseSummary).toList();
    }


    @GetMapping("/{id}/submissions")
    public List<SubmissionResponse> submissions(@PathVariable Long id) {
        return assignmentService.getSubmissionsOfStudent(id).stream().map(DtoMapper::toSubmission).toList();
    }


    @GetMapping("/{id}/quiz-results")
    public List<QuizResultResponse> quizResults(@PathVariable Long id) {
        return quizService.getResultsOfStudent(id).stream().map(DtoMapper::toQuizResult).toList();
    }
}
