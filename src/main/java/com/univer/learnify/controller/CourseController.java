package com.univer.learnify.controller;

import com.univer.learnify.dto.request.CreateCourseRequest;
import com.univer.learnify.dto.request.CreateModuleRequest;
import com.univer.learnify.dto.request.CreateReviewRequest;
import com.univer.learnify.dto.request.UpdateCourseRequest;
import com.univer.learnify.dto.response.*;
import com.univer.learnify.mapper.DtoMapper;
import com.univer.learnify.service.CourseService;
import com.univer.learnify.service.EnrollmentService;
import com.univer.learnify.service.QuizService;
import com.univer.learnify.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final EnrollmentService enrollmentService;
    private final ReviewService reviewService;
    private final QuizService quizService;


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseDetailsResponse create(@Valid @RequestBody CreateCourseRequest request) {
        Long id = courseService.createCourse(request).getId();
        return DtoMapper.toCourseDetails(courseService.getCourseDetails(id));
    }

    @GetMapping
    public List<CourseSummaryResponse> list(@RequestParam(required = false) Long categoryId,
                                            @RequestParam(required = false) Long teacherId,
                                            @RequestParam(required = false) String tag,
                                            @RequestParam(required = false) String title) {
        return courseService.getCourses(categoryId, teacherId, tag, title).stream()
                .map(DtoMapper::toCourseSummary)
                .toList();
    }

    @GetMapping("/{id}")
    public CourseDetailsResponse get(@PathVariable Long id) {
        return DtoMapper.toCourseDetails(courseService.getCourseDetails(id));
    }

    @PutMapping("/{id}")
    public CourseDetailsResponse update(@PathVariable Long id, @Valid @RequestBody UpdateCourseRequest request) {
        courseService.updateCourse(id, request);
        return DtoMapper.toCourseDetails(courseService.getCourseDetails(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        courseService.deleteCourse(id);
    }


    @PostMapping("/{courseId}/modules")
    @ResponseStatus(HttpStatus.CREATED)
    public ModuleResponse addModule(@PathVariable Long courseId, @Valid @RequestBody CreateModuleRequest request) {
        return DtoMapper.toModule(courseService.addModule(courseId, request));
    }


    @PostMapping("/{courseId}/enroll")
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentResponse enroll(@PathVariable Long courseId, @RequestParam("user") Long studentId) {
        return DtoMapper.toEnrollment(enrollmentService.enrollStudent(courseId, studentId));
    }

    @DeleteMapping("/{courseId}/enroll")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unenroll(@PathVariable Long courseId, @RequestParam("user") Long studentId) {
        enrollmentService.unenrollStudent(courseId, studentId);
    }

    @GetMapping("/{courseId}/students")
    public List<UserResponse> students(@PathVariable Long courseId) {
        return enrollmentService.getStudentsOfCourse(courseId).stream().map(DtoMapper::toUser).toList();
    }


    @PostMapping("/{courseId}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse addReview(@PathVariable Long courseId, @Valid @RequestBody CreateReviewRequest request) {
        return DtoMapper.toReview(reviewService.createReview(courseId, request));
    }

    @GetMapping("/{courseId}/reviews")
    public List<ReviewResponse> reviews(@PathVariable Long courseId) {
        return reviewService.getReviewsOfCourse(courseId).stream().map(DtoMapper::toReview).toList();
    }

    @GetMapping("/{courseId}/rating")
    public RatingResponse rating(@PathVariable Long courseId) {
        return new RatingResponse(courseId, reviewService.getAverageRating(courseId));
    }


    @GetMapping("/{courseId}/quiz-results")
    public List<QuizResultResponse> quizResults(@PathVariable Long courseId) {
        return quizService.getResultsOfCourse(courseId).stream().map(DtoMapper::toQuizResult).toList();
    }
}
