package com.univer.learnify.dto.response;

import java.util.List;

public record ModuleResponse(
        Long id,
        String title,
        Integer orderIndex,
        String description,
        List<LessonResponse> lessons
) {
}
