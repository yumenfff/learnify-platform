package com.univer.learnify.controller;

import com.univer.learnify.dto.request.SubmitAssignmentRequest;
import com.univer.learnify.dto.response.AssignmentResponse;
import com.univer.learnify.dto.response.SubmissionResponse;
import com.univer.learnify.mapper.DtoMapper;
import com.univer.learnify.service.AssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    @GetMapping("/{id}")
    public AssignmentResponse get(@PathVariable Long id) {
        return DtoMapper.toAssignment(assignmentService.getAssignment(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        assignmentService.deleteAssignment(id);
    }

    @PostMapping("/{id}/submit")
    @ResponseStatus(HttpStatus.CREATED)
    public SubmissionResponse submit(@PathVariable Long id, @Valid @RequestBody SubmitAssignmentRequest request) {
        return DtoMapper.toSubmission(assignmentService.submitAssignment(id, request));
    }


    @GetMapping("/{id}/submissions")
    public List<SubmissionResponse> submissions(@PathVariable Long id) {
        return assignmentService.getSubmissionsOfAssignment(id).stream()
                .map(DtoMapper::toSubmission)
                .toList();
    }
}
