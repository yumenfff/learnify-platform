package com.univer.learnify.repository;

import com.univer.learnify.entity.Course;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {


    @Override
    @EntityGraph(attributePaths = {"category", "teacher"})
    List<Course> findAll();

    @EntityGraph(attributePaths = {"category", "teacher"})
    List<Course> findByCategoryId(Long categoryId);

    @EntityGraph(attributePaths = {"category", "teacher"})
    List<Course> findByTeacherId(Long teacherId);

    @EntityGraph(attributePaths = {"category", "teacher"})
    List<Course> findByTitleContainingIgnoreCase(String title);

    @EntityGraph(attributePaths = {"category", "teacher"})
    List<Course> findDistinctByTagsName(String tagName);


    @Query("select c from Course c join fetch c.category join fetch c.teacher "
            + "left join fetch c.modules where c.id = :id")
    Optional<Course> findByIdWithModules(@Param("id") Long id);

    @Query("select c from Course c left join fetch c.tags where c.id = :id")
    Optional<Course> findByIdWithTags(@Param("id") Long id);


    @Query("select c from Course c join fetch c.category join fetch c.teacher "
            + "where c.id in (select e.course.id from Enrollment e where e.user.id = :userId) "
            + "order by c.title")
    List<Course> findCoursesByStudentId(@Param("userId") Long userId);
}
