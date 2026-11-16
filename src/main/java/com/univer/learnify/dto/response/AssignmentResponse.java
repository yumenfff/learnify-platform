package com.univer.learnify.dto.response;

import java.time.Instant;

public record AssignmentResponse(
        Long id,
        Long lessonId,
        String title,
        String description,
        Instant dueDate,
        Integer maxScore
) {
}
