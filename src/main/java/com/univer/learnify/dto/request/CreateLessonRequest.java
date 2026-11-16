package com.univer.learnify.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateLessonRequest(
        @NotBlank @Size(max = 200) String title,
        @NotNull @Positive Integer orderIndex,
        String content,
        @Size(max = 500) String videoUrl
) {
}
