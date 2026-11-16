package com.univer.learnify.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record CreateCourseRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 4000) String description,
        @NotNull Long categoryId,
        @NotNull Long teacherId,
        @Positive Integer durationHours,
        LocalDate startDate,
        Set<@NotBlank @Size(max = 50) String> tags,
        @Valid List<CreateModuleRequest> modules
) {
}
