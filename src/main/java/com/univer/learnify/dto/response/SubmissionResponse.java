package com.univer.learnify.dto.response;

import com.univer.learnify.entity.enums.SubmissionStatus;

import java.time.Instant;

public record SubmissionResponse(
        Long id,
        Long assignmentId,
        Long studentId,
        Instant submittedAt,
        String content,
        Integer score,
        String feedback,
        SubmissionStatus status
) {
}
