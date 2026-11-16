package com.univer.learnify.repository;

import com.univer.learnify.entity.Enrollment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    Optional<Enrollment> findByUserIdAndCourseId(Long userId, Long courseId);


    @EntityGraph(attributePaths = "user")
    List<Enrollment> findByCourseId(Long courseId);


    @EntityGraph(attributePaths = "course")
    List<Enrollment> findByUserId(Long userId);

    boolean existsByCourseId(Long courseId);

    long countByCourseId(Long courseId);
}
