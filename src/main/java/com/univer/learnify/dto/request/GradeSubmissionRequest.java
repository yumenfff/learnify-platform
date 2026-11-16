package com.univer.learnify.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record GradeSubmissionRequest(
        @NotNull @PositiveOrZero Integer score,
        @Size(max = 2000) String feedback
) {
}
