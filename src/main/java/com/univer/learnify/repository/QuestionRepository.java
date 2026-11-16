package com.univer.learnify.repository;

import com.univer.learnify.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {


    @Query("select distinct q from Question q left join fetch q.options "
            + "where q.quiz.id = :quizId order by q.id")
    List<Question> findByQuizIdWithOptions(@Param("quizId") Long quizId);
}
