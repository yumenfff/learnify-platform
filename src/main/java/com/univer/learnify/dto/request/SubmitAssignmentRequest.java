package com.univer.learnify.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubmitAssignmentRequest(
        @NotNull Long studentId,
        @NotBlank String content
) {
}
