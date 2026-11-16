package com.univer.learnify.dto.response;

import com.univer.learnify.entity.enums.QuestionType;

import java.util.List;

public record QuestionResponse(
        Long id,
        String text,
        QuestionType type,
        List<OptionResponse> options
) {
}
