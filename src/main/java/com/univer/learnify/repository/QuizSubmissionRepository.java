package com.univer.learnify.repository;

import com.univer.learnify.entity.QuizSubmission;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizSubmissionRepository extends JpaRepository<QuizSubmission, Long> {


    @EntityGraph(attributePaths = "quiz")
    List<QuizSubmission> findByStudentId(Long studentId);


    @EntityGraph(attributePaths = "student")
    List<QuizSubmission> findByQuizId(Long quizId);


    @EntityGraph(attributePaths = {"student", "quiz"})
    List<QuizSubmission> findByQuizModuleCourseId(Long courseId);

    List<QuizSubmission> findByQuizIdAndStudentId(Long quizId, Long studentId);
}
