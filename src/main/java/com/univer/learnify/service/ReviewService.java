package com.univer.learnify.service;

import com.univer.learnify.dto.request.CreateReviewRequest;
import com.univer.learnify.entity.Course;
import com.univer.learnify.entity.CourseReview;
import com.univer.learnify.entity.User;
import com.univer.learnify.entity.enums.Role;
import com.univer.learnify.exception.BusinessRuleException;
import com.univer.learnify.exception.ConflictException;
import com.univer.learnify.exception.ResourceNotFoundException;
import com.univer.learnify.repository.CourseRepository;
import com.univer.learnify.repository.CourseReviewRepository;
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
public class ReviewService {

    private final CourseReviewRepository reviewRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;


    @Transactional
    public CourseReview createReview(Long courseId, CreateReviewRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Курс", courseId));
        User student = userRepository.findById(request.studentId())
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь", request.studentId()));

        if (student.getRole() != Role.STUDENT) {
            throw new BusinessRuleException("Пользователь id=" + student.getId() + " не является студентом");
        }
        if (!enrollmentRepository.existsByUserIdAndCourseId(student.getId(), courseId)) {
            throw new BusinessRuleException(
                    "Студент id=" + student.getId() + " не записан на курс id=" + courseId);
        }
        if (reviewRepository.existsByCourseIdAndStudentId(courseId, student.getId())) {
            throw new ConflictException("Студент id=" + student.getId()
                    + " уже оставил отзыв на курс id=" + courseId);
        }

        CourseReview saved = reviewRepository.save(
                new CourseReview(course, student, request.rating(), request.comment()));
        log.info("Студент id={} оставил отзыв id={} на курс id={}", student.getId(), saved.getId(), courseId);
        return saved;
    }


    public List<CourseReview> getReviewsOfCourse(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException("Курс", courseId);
        }
        return reviewRepository.findByCourseId(courseId);
    }


    public Double getAverageRating(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException("Курс", courseId);
        }
        return reviewRepository.findAverageRating(courseId);
    }

    @Transactional
    public void deleteReview(Long reviewId) {
        CourseReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Отзыв", reviewId));
        reviewRepository.delete(review);
        log.info("Удалён отзыв id={}", reviewId);
    }
}
