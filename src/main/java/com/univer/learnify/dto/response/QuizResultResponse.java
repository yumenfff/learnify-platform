package com.univer.learnify.dto.response;

import java.time.Instant;

public record QuizResultResponse(
        Long id,
        Long quizId,
        Long studentId,
        Integer score,
        Instant takenAt
) {
}
