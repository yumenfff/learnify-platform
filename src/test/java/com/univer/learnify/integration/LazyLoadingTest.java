package com.univer.learnify.integration;

import com.univer.learnify.AbstractIntegrationTest;
import com.univer.learnify.entity.Course;
import com.univer.learnify.repository.CourseRepository;
import com.univer.learnify.service.CourseService;
import com.univer.learnify.support.TestDataFactory;
import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;


class LazyLoadingTest extends AbstractIntegrationTest {

    @Autowired
    TestDataFactory factory;
    @Autowired
    CourseRepository courseRepository;
    @Autowired
    CourseService courseService;

    private Long courseId;

    @BeforeEach
    void setUp() {
        courseId = factory.createCourse(factory.createTeacher()).getId();
    }

    @Test
    void lazyCollectionOutsideTransaction_throwsLazyInitializationException() {
        Course course = courseRepository.findById(courseId).orElseThrow();

        assertThrows(LazyInitializationException.class, () -> course.getModules().size());
    }

    @Test
    void lazyManyToOneOutsideTransaction_throwsLazyInitializationException() {
        Course course = courseRepository.findById(courseId).orElseThrow();

        assertThrows(LazyInitializationException.class, () -> course.getTeacher().getName());
    }

    @Test
    void joinFetch_loadsCollectionInSingleQuery() {
        Course course = courseRepository.findByIdWithModules(courseId).orElseThrow();

        assertEquals(1, course.getModules().size());
        assertNotNull(course.getTeacher().getName());
    }

    @Test
    void transactionalService_loadsWholeStructure() {
        Course course = courseService.getCourseDetails(courseId);

        assertEquals(2, course.getModules().get(0).getLessons().size());
        assertEquals(1, course.getTags().size());
    }
}
