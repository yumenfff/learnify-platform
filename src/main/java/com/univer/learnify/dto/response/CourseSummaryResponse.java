package com.univer.learnify.dto.response;

import java.time.LocalDate;

public record CourseSummaryResponse(
        Long id,
        String title,
        String description,
        Integer durationHours,
        LocalDate startDate,
        Long categoryId,
        String categoryName,
        Long teacherId,
        String teacherName
) {
}
