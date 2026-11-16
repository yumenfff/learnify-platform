package com.univer.learnify.repository;

import com.univer.learnify.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    Optional<Quiz> findByModuleId(Long moduleId);

    boolean existsByModuleId(Long moduleId);


    @Query("select q from Quiz q left join fetch q.questions where q.id = :id")
    Optional<Quiz> findByIdWithQuestions(@Param("id") Long id);
}
