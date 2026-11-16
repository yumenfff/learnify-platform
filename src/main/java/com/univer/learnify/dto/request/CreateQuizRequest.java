package com.univer.learnify.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateQuizRequest(
        @NotBlank @Size(max = 200) String title,
        @Positive Integer timeLimitMinutes,
        @NotEmpty @Valid List<CreateQuestionRequest> questions
) {
}
