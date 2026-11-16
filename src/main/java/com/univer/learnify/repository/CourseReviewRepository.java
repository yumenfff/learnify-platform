package com.univer.learnify.repository;

import com.univer.learnify.entity.CourseReview;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseReviewRepository extends JpaRepository<CourseReview, Long> {

    @EntityGraph(attributePaths = "student")
    List<CourseReview> findByCourseId(Long courseId);

    boolean existsByCourseIdAndStudentId(Long courseId, Long studentId);


    @Query("select avg(r.rating) from CourseReview r where r.course.id = :courseId")
    Double findAverageRating(@Param("courseId") Long courseId);
}
