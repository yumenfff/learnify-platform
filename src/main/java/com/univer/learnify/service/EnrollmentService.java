package com.univer.learnify.service;

import com.univer.learnify.entity.Course;
import com.univer.learnify.entity.Enrollment;
import com.univer.learnify.entity.User;
import com.univer.learnify.entity.enums.Role;
import com.univer.learnify.exception.BusinessRuleException;
import com.univer.learnify.exception.ConflictException;
import com.univer.learnify.exception.ResourceNotFoundException;
import com.univer.learnify.repository.CourseRepository;
import com.univer.learnify.repository.EnrollmentRepository;
import com.univer.learnify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;


    @Transactional
    public Enrollment enrollStudent(Long courseId, Long studentId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Курс", courseId));
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь", studentId));

        if (student.getRole() != Role.STUDENT) {
            throw new BusinessRuleException("Пользователь id=" + studentId + " не является студентом");
        }
        if (enrollmentRepository.existsByUserIdAndCourseId(studentId, courseId)) {
            throw new ConflictException("Студент id=" + studentId + " уже записан на курс id=" + courseId);
        }

        Enrollment saved = enrollmentRepository.save(new Enrollment(student, course));
        log.info("Студент id={} записан на курс id={}", studentId, courseId);
        return saved;
    }


    @Transactional
    public void unenrollStudent(Long courseId, Long studentId) {
        Enrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(studentId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Запись студента id=" + studentId + " на курс", courseId));
        enrollmentRepository.delete(enrollment);
        log.info("Студент id={} отписан от курса id={}", studentId, courseId);
    }


    public List<User> getStudentsOfCourse(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException("Курс", courseId);
        }
        return userRepository.findStudentsByCourseId(courseId);
    }


    public List<Course> getCoursesOfStudent(Long studentId) {
        if (!userRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Пользователь", studentId);
        }
        return courseRepository.findCoursesByStudentId(studentId);
    }
}
