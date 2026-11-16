package com.univer.learnify.repository;

import com.univer.learnify.entity.Submission;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    boolean existsByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    Optional<Submission> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);


    @EntityGraph(attributePaths = "student")
    List<Submission> findByAssignmentId(Long assignmentId);


    @EntityGraph(attributePaths = "assignment")
    List<Submission> findByStudentId(Long studentId);

    boolean existsByAssignmentId(Long assignmentId);
}
