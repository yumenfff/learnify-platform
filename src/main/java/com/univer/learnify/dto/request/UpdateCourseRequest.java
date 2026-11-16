package com.univer.learnify.dto.request;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;


public record UpdateCourseRequest(
        @Size(min = 1, max = 200) String title,
        @Size(max = 4000) String description,
        Long categoryId,
        Long teacherId,
        @Positive Integer durationHours,
        LocalDate startDate
) {
}
