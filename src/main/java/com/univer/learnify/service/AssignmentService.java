package com.univer.learnify.service;

import com.univer.learnify.dto.request.CreateAssignmentRequest;
import com.univer.learnify.dto.request.GradeSubmissionRequest;
import com.univer.learnify.dto.request.SubmitAssignmentRequest;
import com.univer.learnify.entity.Assignment;
import com.univer.learnify.entity.Lesson;
import com.univer.learnify.entity.Submission;
import com.univer.learnify.entity.User;
import com.univer.learnify.entity.enums.Role;
import com.univer.learnify.entity.enums.SubmissionStatus;
import com.univer.learnify.exception.BusinessRuleException;
import com.univer.learnify.exception.ConflictException;
import com.univer.learnify.exception.ResourceNotFoundException;
import com.univer.learnify.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;


    @Transactional
    public Assignment createAssignment(Long lessonId, CreateAssignmentRequest request) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Урок", lessonId));
        Assignment assignment = new Assignment(
                request.title(), request.description(), request.dueDate(), request.maxScore());
        lesson.addAssignment(assignment);
        Assignment saved = assignmentRepository.save(assignment);
        log.info("К уроку id={} добавлено задание id={}", lessonId, saved.getId());
        return saved;
    }

    public Assignment getAssignment(Long id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Задание", id));
    }

    public List<Assignment> getAssignmentsOfLesson(Long lessonId) {
        if (!lessonRepository.existsById(lessonId)) {
            throw new ResourceNotFoundException("Урок", lessonId);
        }
        return assignmentRepository.findByLessonId(lessonId);
    }


    @Transactional
    public void deleteAssignment(Long id) {
        Assignment assignment = getAssignment(id);
        if (submissionRepository.existsByAssignmentId(id)) {
            throw new ConflictException("Нельзя удалить задание id=" + id + ": по нему есть решения");
        }
        assignmentRepository.delete(assignment);
        log.info("Удалено задание id={}", id);
    }


    @Transactional
    public Submission submitAssignment(Long assignmentId, SubmitAssignmentRequest request) {
        Assignment assignment = getAssignment(assignmentId);
        User student = userRepository.findById(request.studentId())
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь", request.studentId()));

        if (student.getRole() != Role.STUDENT) {
            throw new BusinessRuleException("Пользователь id=" + student.getId() + " не является студентом");
        }

        Long courseId = assignment.getLesson().getModule().getCourse().getId();
        if (!enrollmentRepository.existsByUserIdAndCourseId(student.getId(), courseId)) {
            throw new BusinessRuleException(
                    "Студент id=" + student.getId() + " не записан на курс id=" + courseId);
        }
        if (assignment.getDueDate() != null && Instant.now().isAfter(assignment.getDueDate())) {
            throw new BusinessRuleException("Срок сдачи задания id=" + assignmentId + " истёк");
        }
        if (submissionRepository.existsByAssignmentIdAndStudentId(assignmentId, student.getId())) {
            throw new ConflictException("Студент id=" + student.getId()
                    + " уже отправил решение на задание id=" + assignmentId);
        }

        Submission saved = submissionRepository.save(new Submission(assignment, student, request.content()));
        log.info("Студент id={} отправил решение id={} на задание id={}",
                student.getId(), saved.getId(), assignmentId);
        return saved;
    }


    @Transactional
    public Submission gradeSubmission(Long submissionId, GradeSubmissionRequest request) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Решение", submissionId));

        int maxScore = submission.getAssignment().getMaxScore();
        if (request.score() > maxScore) {
            throw new BusinessRuleException(
                    "Оценка " + request.score() + " больше максимальной (" + maxScore + ")");
        }

        submission.setScore(request.score());
        submission.setFeedback(request.feedback());
        submission.setStatus(SubmissionStatus.GRADED);
        log.info("Решение id={} оценено: {}/{}", submissionId, request.score(), maxScore);
        return submission;
    }


    public List<Submission> getSubmissionsOfAssignment(Long assignmentId) {
        if (!assignmentRepository.existsById(assignmentId)) {
            throw new ResourceNotFoundException("Задание", assignmentId);
        }
        return submissionRepository.findByAssignmentId(assignmentId);
    }


    public List<Submission> getSubmissionsOfStudent(Long studentId) {
        if (!userRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Пользователь", studentId);
        }
        return submissionRepository.findByStudentId(studentId);
    }
}
