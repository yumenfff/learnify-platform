package com.univer.learnify.dto.response;

public record LessonResponse(
        Long id,
        String title,
        Integer orderIndex,
        String content,
        String videoUrl
) {
}
