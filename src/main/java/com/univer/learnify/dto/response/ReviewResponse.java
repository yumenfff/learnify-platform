package com.univer.learnify.dto.response;

import java.time.Instant;

public record ReviewResponse(
        Long id,
        Long courseId,
        Long studentId,
        String studentName,
        Integer rating,
        String comment,
        Instant createdAt
) {
}
