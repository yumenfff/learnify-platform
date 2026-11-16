package com.univer.learnify.repository;

import com.univer.learnify.entity.Module;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ModuleRepository extends JpaRepository<Module, Long> {

    List<Module> findByCourseIdOrderByOrderIndex(Long courseId);


    @Query("select distinct m from Module m left join fetch m.lessons "
            + "where m.course.id = :courseId order by m.orderIndex")
    List<Module> findByCourseIdWithLessons(@Param("courseId") Long courseId);
}
