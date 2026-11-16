package com.univer.learnify.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateModuleRequest(
        @NotBlank @Size(max = 200) String title,
        @NotNull @Positive Integer orderIndex,
        @Size(max = 2000) String description,
        @Valid List<CreateLessonRequest> lessons
) {
}
