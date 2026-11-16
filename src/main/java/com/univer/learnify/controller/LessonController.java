package com.univer.learnify.controller;

import com.univer.learnify.dto.request.CreateAssignmentRequest;
import com.univer.learnify.dto.response.AssignmentResponse;
import com.univer.learnify.mapper.DtoMapper;
import com.univer.learnify.service.AssignmentService;
import com.univer.learnify.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final CourseService courseService;
    private final AssignmentService assignmentService;

    @DeleteMapping("/{lessonId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long lessonId) {
        courseService.deleteLesson(lessonId);
    }

    @PostMapping("/{lessonId}/assignments")
    @ResponseStatus(HttpStatus.CREATED)
    public AssignmentResponse createAssignment(@PathVariable Long lessonId,
                                               @Valid @RequestBody CreateAssignmentRequest request) {
        return DtoMapper.toAssignment(assignmentService.createAssignment(lessonId, request));
    }

    @GetMapping("/{lessonId}/assignments")
    public List<AssignmentResponse> assignments(@PathVariable Long lessonId) {
        return assignmentService.getAssignmentsOfLesson(lessonId).stream()
                .map(DtoMapper::toAssignment)
                .toList();
    }
}
