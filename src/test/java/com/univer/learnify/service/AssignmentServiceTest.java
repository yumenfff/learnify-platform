package com.univer.learnify.service;

import com.univer.learnify.dto.request.GradeSubmissionRequest;
import com.univer.learnify.dto.request.SubmitAssignmentRequest;
import com.univer.learnify.entity.*;
import com.univer.learnify.entity.Module;
import com.univer.learnify.entity.enums.Role;
import com.univer.learnify.entity.enums.SubmissionStatus;
import com.univer.learnify.exception.BusinessRuleException;
import com.univer.learnify.exception.ConflictException;
import com.univer.learnify.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

    @Mock
    AssignmentRepository assignmentRepository;
    @Mock
    SubmissionRepository submissionRepository;
    @Mock
    LessonRepository lessonRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    EnrollmentRepository enrollmentRepository;
    @InjectMocks
    AssignmentService service;

    private User student;
    private Assignment assignment;

    @BeforeEach
    void setUp() {
        Course course = new Course();
        course.setId(1L);
        Module module = new Module();
        module.setCourse(course);
        Lesson lesson = new Lesson();
        lesson.setModule(module);

        assignment = new Assignment("HW", null, Instant.now().plusSeconds(3600), 100);
        assignment.setId(5L);
        assignment.setLesson(lesson);

        student = new User("Student", "s@test.dev", Role.STUDENT);
        student.setId(3L);
    }

    private void stubSubmitPreconditions() {
        when(assignmentRepository.findById(5L)).thenReturn(Optional.of(assignment));
        when(userRepository.findById(3L)).thenReturn(Optional.of(student));
        when(enrollmentRepository.existsByUserIdAndCourseId(3L, 1L)).thenReturn(true);
    }

    @Test
    void submitAssignment_afterDeadline_throwsBusinessRule() {
        assignment.setDueDate(Instant.now().minusSeconds(60));
        stubSubmitPreconditions();

        assertThrows(BusinessRuleException.class,
                () -> service.submitAssignment(5L, new SubmitAssignmentRequest(3L, "решение")));
        verify(submissionRepository, never()).save(any());
    }

    @Test
    void submitAssignment_duplicate_throwsConflict() {
        stubSubmitPreconditions();
        when(submissionRepository.existsByAssignmentIdAndStudentId(5L, 3L)).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> service.submitAssignment(5L, new SubmitAssignmentRequest(3L, "решение")));
        verify(submissionRepository, never()).save(any());
    }

    @Test
    void gradeSubmission_scoreAboveMax_throwsBusinessRule() {
        Submission submission = new Submission(assignment, student, "решение");
        when(submissionRepository.findById(7L)).thenReturn(Optional.of(submission));

        assertThrows(BusinessRuleException.class,
                () -> service.gradeSubmission(7L, new GradeSubmissionRequest(150, null)));
        assertEquals(SubmissionStatus.SUBMITTED, submission.getStatus());
    }

    @Test
    void gradeSubmission_valid_setsScoreFeedbackAndStatus() {
        Submission submission = new Submission(assignment, student, "решение");
        when(submissionRepository.findById(7L)).thenReturn(Optional.of(submission));

        Submission result = service.gradeSubmission(7L, new GradeSubmissionRequest(80, "Хорошо"));

        assertEquals(80, result.getScore());
        assertEquals("Хорошо", result.getFeedback());
        assertEquals(SubmissionStatus.GRADED, result.getStatus());
    }
}
