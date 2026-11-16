package com.univer.learnify.service;

import com.univer.learnify.entity.Course;
import com.univer.learnify.entity.Enrollment;
import com.univer.learnify.entity.User;
import com.univer.learnify.entity.enums.EnrollmentStatus;
import com.univer.learnify.entity.enums.Role;
import com.univer.learnify.exception.BusinessRuleException;
import com.univer.learnify.exception.ConflictException;
import com.univer.learnify.exception.ResourceNotFoundException;
import com.univer.learnify.repository.CourseRepository;
import com.univer.learnify.repository.EnrollmentRepository;
import com.univer.learnify.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    EnrollmentRepository enrollmentRepository;
    @Mock
    CourseRepository courseRepository;
    @Mock
    UserRepository userRepository;
    @InjectMocks
    EnrollmentService service;

    private Course course;
    private User student;

    @BeforeEach
    void setUp() {
        course = new Course();
        course.setId(1L);
        student = new User("Student", "s@test.dev", Role.STUDENT);
        student.setId(2L);
    }

    @Test
    void enrollStudent_success_createsActiveEnrollment() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(userRepository.findById(2L)).thenReturn(Optional.of(student));
        when(enrollmentRepository.existsByUserIdAndCourseId(2L, 1L)).thenReturn(false);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        Enrollment result = service.enrollStudent(1L, 2L);

        assertEquals(EnrollmentStatus.ACTIVE, result.getStatus());
        assertSame(course, result.getCourse());
        assertSame(student, result.getUser());
    }

    @Test
    void enrollStudent_duplicate_throwsConflict() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(userRepository.findById(2L)).thenReturn(Optional.of(student));
        when(enrollmentRepository.existsByUserIdAndCourseId(2L, 1L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.enrollStudent(1L, 2L));
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void enrollStudent_teacher_throwsBusinessRule() {
        student.setRole(Role.TEACHER);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(userRepository.findById(2L)).thenReturn(Optional.of(student));

        assertThrows(BusinessRuleException.class, () -> service.enrollStudent(1L, 2L));
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void enrollStudent_courseNotFound_throwsNotFound() {
        when(courseRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.enrollStudent(1L, 2L));
    }

    @Test
    void unenrollStudent_success_deletesEnrollment() {
        Enrollment enrollment = new Enrollment(student, course);
        when(enrollmentRepository.findByUserIdAndCourseId(2L, 1L)).thenReturn(Optional.of(enrollment));

        service.unenrollStudent(1L, 2L);

        verify(enrollmentRepository).delete(enrollment);
    }

    @Test
    void unenrollStudent_notEnrolled_throwsNotFound() {
        when(enrollmentRepository.findByUserIdAndCourseId(2L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.unenrollStudent(1L, 2L));
    }
}
