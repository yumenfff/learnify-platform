package com.univer.learnify.dto.request;

import com.univer.learnify.entity.enums.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateQuestionRequest(
        @NotBlank @Size(max = 1000) String text,
        @NotNull QuestionType type,
        @NotNull @Size(min = 2) @Valid List<CreateOptionRequest> options
) {
}
