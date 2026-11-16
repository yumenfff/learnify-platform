package com.univer.learnify.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.Set;


public record TakeQuizRequest(
        @NotNull Long studentId,
        @NotNull Map<Long, Set<Long>> answers
) {
}
