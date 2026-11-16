package com.univer.learnify.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateAssignmentRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 4000) String description,
        Instant dueDate,
        @NotNull @Positive Integer maxScore
) {
}
