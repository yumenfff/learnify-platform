package com.univer.learnify.integration;

import com.univer.learnify.AbstractIntegrationTest;
import com.univer.learnify.dto.request.*;
import com.univer.learnify.entity.*;
import com.univer.learnify.entity.enums.QuestionType;
import com.univer.learnify.entity.enums.SubmissionStatus;
import com.univer.learnify.exception.BusinessRuleException;
import com.univer.learnify.exception.ConflictException;
import com.univer.learnify.exception.ResourceNotFoundException;
import com.univer.learnify.service.*;
import com.univer.learnify.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


class ServiceFlowTest extends AbstractIntegrationTest {

    @Autowired
    TestDataFactory factory;
    @Autowired
    EnrollmentService enrollmentService;
    @Autowired
    CourseService courseService;
    @Autowired
    AssignmentService assignmentService;
    @Autowired
    QuizService quizService;
    @Autowired
    ReviewService reviewService;

    private User teacher;
    private User student;
    private Course course;
    private Long moduleId;
    private Long lessonId;

    @BeforeEach
    void setUp() {
        teacher = factory.createTeacher();
        student = factory.createStudent();
        course = factory.createCourse(teacher);
        moduleId = factory.firstModuleId(course);
        lessonId = factory.firstLessonId(course);
    }

    @Test
    void enroll_twice_throwsConflict() {
        enrollmentService.enrollStudent(course.getId(), student.getId());

        assertThrows(ConflictException.class,
                () -> enrollmentService.enrollStudent(course.getId(), student.getId()));
        assertEquals(1, enrollmentService.getStudentsOfCourse(course.getId()).size());
    }

    @Test
    void enroll_teacher_throwsBusinessRule() {
        assertThrows(BusinessRuleException.class,
                () -> enrollmentService.enrollStudent(course.getId(), teacher.getId()));
    }

    @Test
    void deleteCourse_blockedByEnrollment_allowedAfterUnenroll() {
        enrollmentService.enrollStudent(course.getId(), student.getId());

        assertThrows(ConflictException.class, () -> courseService.deleteCourse(course.getId()));

        enrollmentService.unenrollStudent(course.getId(), student.getId());
        courseService.deleteCourse(course.getId());

        assertThrows(ResourceNotFoundException.class, () -> courseService.getCourseDetails(course.getId()));
    }

    @Test
    void submitAssignment_withoutEnrollment_throwsBusinessRule() {
        Assignment assignment = assignmentService.createAssignment(lessonId,
                new CreateAssignmentRequest("HW", null, null, 100));

        assertThrows(BusinessRuleException.class, () -> assignmentService.submitAssignment(
                assignment.getId(), new SubmitAssignmentRequest(student.getId(), "решение")));
    }

    @Test
    void assignmentCycle_submitDuplicateAndGrade() {
        enrollmentService.enrollStudent(course.getId(), student.getId());
        Assignment assignment = assignmentService.createAssignment(lessonId,
                new CreateAssignmentRequest("HW", "описание", null, 100));

        Submission submission = assignmentService.submitAssignment(
                assignment.getId(), new SubmitAssignmentRequest(student.getId(), "решение"));
        assertEquals(SubmissionStatus.SUBMITTED, submission.getStatus());

        assertThrows(ConflictException.class, () -> assignmentService.submitAssignment(
                assignment.getId(), new SubmitAssignmentRequest(student.getId(), "ещё раз")));
        assertThrows(BusinessRuleException.class, () -> assignmentService.gradeSubmission(
                submission.getId(), new GradeSubmissionRequest(101, null)));

        Submission graded = assignmentService.gradeSubmission(
                submission.getId(), new GradeSubmissionRequest(90, "Хорошо"));
        assertEquals(SubmissionStatus.GRADED, graded.getStatus());
        assertEquals(90, graded.getScore());
        assertEquals(1, assignmentService.getSubmissionsOfAssignment(assignment.getId()).size());
    }

    @Test
    void quiz_takeCalculatesScoreAndStoresResults() {
        enrollmentService.enrollStudent(course.getId(), student.getId());
        Quiz quiz = quizService.createQuiz(moduleId, new CreateQuizRequest("Quiz", null, List.of(
                new CreateQuestionRequest("Вопрос?", QuestionType.SINGLE_CHOICE, List.of(
                        new CreateOptionRequest("верно", true),
                        new CreateOptionRequest("неверно", false))))));

        assertThrows(ConflictException.class, () -> quizService.createQuiz(moduleId,
                new CreateQuizRequest("Второй", null, List.of(
                        new CreateQuestionRequest("Q", QuestionType.SINGLE_CHOICE, List.of(
                                new CreateOptionRequest("a", true),
                                new CreateOptionRequest("b", false)))))));

        Question question = quizService.getQuiz(quiz.getId()).getQuestions().get(0);
        Long right = question.getOptions().stream().filter(AnswerOption::isCorrect).findFirst().orElseThrow().getId();
        Long wrong = question.getOptions().stream().filter(o -> !o.isCorrect()).findFirst().orElseThrow().getId();

        QuizSubmission good = quizService.takeQuiz(quiz.getId(),
                new TakeQuizRequest(student.getId(), Map.of(question.getId(), Set.of(right))));
        QuizSubmission bad = quizService.takeQuiz(quiz.getId(),
                new TakeQuizRequest(student.getId(), Map.of(question.getId(), Set.of(wrong))));

        assertEquals(100, good.getScore());
        assertEquals(0, bad.getScore());
        assertEquals(2, quizService.getResultsOfStudent(student.getId()).size());
    }

    @Test
    void review_oncePerStudent_andAverageRating() {
        enrollmentService.enrollStudent(course.getId(), student.getId());

        reviewService.createReview(course.getId(), new CreateReviewRequest(student.getId(), 4, "Хороший курс"));

        assertThrows(ConflictException.class, () -> reviewService.createReview(
                course.getId(), new CreateReviewRequest(student.getId(), 5, "Ещё раз")));
        assertEquals(4.0, reviewService.getAverageRating(course.getId()));
    }
}
