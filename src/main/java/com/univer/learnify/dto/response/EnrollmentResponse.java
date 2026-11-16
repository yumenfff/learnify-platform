package com.univer.learnify.dto.response;

import com.univer.learnify.entity.enums.EnrollmentStatus;

import java.time.LocalDate;

public record EnrollmentResponse(
        Long id,
        Long courseId,
        Long studentId,
        LocalDate enrollDate,
        EnrollmentStatus status
) {
}
