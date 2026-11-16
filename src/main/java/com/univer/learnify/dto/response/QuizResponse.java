package com.univer.learnify.dto.response;

import java.util.List;

public record QuizResponse(
        Long id,
        Long moduleId,
        String title,
        Integer timeLimitMinutes,
        List<QuestionResponse> questions
) {
}
