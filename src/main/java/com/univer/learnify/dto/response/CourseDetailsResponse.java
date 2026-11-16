package com.univer.learnify.dto.response;

import java.time.LocalDate;
import java.util.List;

public record CourseDetailsResponse(
        Long id,
        String title,
        String description,
        Integer durationHours,
        LocalDate startDate,
        Long categoryId,
        String categoryName,
        Long teacherId,
        String teacherName,
        List<String> tags,
        List<ModuleResponse> modules
) {
}
