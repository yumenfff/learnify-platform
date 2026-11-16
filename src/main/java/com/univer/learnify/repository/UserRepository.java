package com.univer.learnify.repository;

import com.univer.learnify.entity.User;
import com.univer.learnify.entity.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRole(Role role);


    @Query("select e.user from Enrollment e where e.course.id = :courseId order by e.user.name")
    List<User> findStudentsByCourseId(@Param("courseId") Long courseId);
}
