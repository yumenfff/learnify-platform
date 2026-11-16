package com.univer.learnify.controller;

import com.univer.learnify.dto.request.GradeSubmissionRequest;
import com.univer.learnify.dto.response.SubmissionResponse;
import com.univer.learnify.mapper.DtoMapper;
import com.univer.learnify.service.AssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final AssignmentService assignmentService;

    @PutMapping("/{id}/grade")
    public SubmissionResponse grade(@PathVariable Long id, @Valid @RequestBody GradeSubmissionRequest request) {
        return DtoMapper.toSubmission(assignmentService.gradeSubmission(id, request));
    }
}
