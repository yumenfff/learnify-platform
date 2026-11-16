package com.univer.learnify.service;

import com.univer.learnify.dto.request.CreateOptionRequest;
import com.univer.learnify.dto.request.CreateQuestionRequest;
import com.univer.learnify.dto.request.CreateQuizRequest;
import com.univer.learnify.dto.request.TakeQuizRequest;
import com.univer.learnify.entity.*;
import com.univer.learnify.entity.Module;
import com.univer.learnify.entity.enums.QuestionType;
import com.univer.learnify.entity.enums.Role;
import com.univer.learnify.exception.BusinessRuleException;
import com.univer.learnify.exception.ConflictException;
import com.univer.learnify.exception.ResourceNotFoundException;
import com.univer.learnify.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuizSubmissionRepository quizSubmissionRepository;
    private final ModuleRepository moduleRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;


    @Transactional
    public Quiz createQuiz(Long moduleId, CreateQuizRequest request) {
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Модуль", moduleId));
        if (quizRepository.existsByModuleId(moduleId)) {
            throw new ConflictException("У модуля id=" + moduleId + " уже есть тест");
        }

        Quiz quiz = new Quiz(request.title(), request.timeLimitMinutes());
        quiz.setModule(module);
        for (CreateQuestionRequest q : request.questions()) {
            validateQuestion(q);
            Question question = new Question(q.text(), q.type());
            for (CreateOptionRequest o : q.options()) {
                question.addOption(new AnswerOption(o.text(), o.correct()));
            }
            quiz.addQuestion(question);
        }

        Quiz saved = quizRepository.save(quiz);
        log.info("Создан тест id={} для модуля id={}, вопросов: {}",
                saved.getId(), moduleId, saved.getQuestions().size());
        return saved;
    }


    public Quiz getQuiz(Long quizId) {
        Quiz quiz = quizRepository.findByIdWithQuestions(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Тест", quizId));

        questionRepository.findByQuizIdWithOptions(quizId);
        return quiz;
    }


    public Quiz getQuizOfModule(Long moduleId) {
        Quiz quiz = quizRepository.findByModuleId(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Тест модуля", moduleId));
        return getQuiz(quiz.getId());
    }


    @Transactional
    public void deleteQuiz(Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Тест", quizId));
        if (!quizSubmissionRepository.findByQuizId(quizId).isEmpty()) {
            throw new ConflictException("Нельзя удалить тест id=" + quizId + ": есть результаты студентов");
        }
        quizRepository.delete(quiz);
        log.info("Удалён тест id={}", quizId);
    }


    @Transactional
    public QuizSubmission takeQuiz(Long quizId, TakeQuizRequest request) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Тест", quizId));
        User student = userRepository.findById(request.studentId())
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь", request.studentId()));

        if (student.getRole() != Role.STUDENT) {
            throw new BusinessRuleException("Пользователь id=" + student.getId() + " не является студентом");
        }
        Long courseId = quiz.getModule().getCourse().getId();
        if (!enrollmentRepository.existsByUserIdAndCourseId(student.getId(), courseId)) {
            throw new BusinessRuleException(
                    "Студент id=" + student.getId() + " не записан на курс id=" + courseId);
        }

        List<Question> questions = questionRepository.findByQuizIdWithOptions(quizId);
        if (questions.isEmpty()) {
            throw new BusinessRuleException("В тесте id=" + quizId + " нет вопросов");
        }
        validateAnswersBelongToQuiz(questions, request);

        int correctCount = 0;
        for (Question question : questions) {
            Set<Long> selected = request.answers().getOrDefault(question.getId(), Collections.emptySet());
            Set<Long> correct = question.getOptions().stream()
                    .filter(AnswerOption::isCorrect)
                    .map(AnswerOption::getId)
                    .collect(Collectors.toSet());
            if (selected.equals(correct)) {
                correctCount++;
            }
        }
        int score = (int) Math.round(100.0 * correctCount / questions.size());

        QuizSubmission saved = quizSubmissionRepository.save(new QuizSubmission(quiz, student, score));
        log.info("Студент id={} прошёл тест id={}: {}% ({}/{})",
                student.getId(), quizId, score, correctCount, questions.size());
        return saved;
    }


    public List<QuizSubmission> getResultsOfStudent(Long studentId) {
        if (!userRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Пользователь", studentId);
        }
        return quizSubmissionRepository.findByStudentId(studentId);
    }


    public List<QuizSubmission> getResultsOfQuiz(Long quizId) {
        if (!quizRepository.existsById(quizId)) {
            throw new ResourceNotFoundException("Тест", quizId);
        }
        return quizSubmissionRepository.findByQuizId(quizId);
    }


    public List<QuizSubmission> getResultsOfCourse(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException("Курс", courseId);
        }
        return quizSubmissionRepository.findByQuizModuleCourseId(courseId);
    }


    private void validateQuestion(CreateQuestionRequest question) {
        long correctCount = question.options().stream().filter(CreateOptionRequest::correct).count();
        if (correctCount == 0) {
            throw new BusinessRuleException("В вопросе «" + question.text() + "» нет верного варианта");
        }
        if (question.type() == QuestionType.SINGLE_CHOICE && correctCount != 1) {
            throw new BusinessRuleException(
                    "В вопросе «" + question.text() + "» с одним ответом должен быть ровно один верный вариант");
        }
    }


    private void validateAnswersBelongToQuiz(List<Question> questions, TakeQuizRequest request) {
        for (var entry : request.answers().entrySet()) {
            Question question = questions.stream()
                    .filter(q -> q.getId().equals(entry.getKey()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessRuleException(
                            "Вопрос id=" + entry.getKey() + " не относится к этому тесту"));
            Set<Long> optionIds = question.getOptions().stream()
                    .map(AnswerOption::getId)
                    .collect(Collectors.toSet());
            if (!optionIds.containsAll(new HashSet<>(entry.getValue()))) {
                throw new BusinessRuleException(
                        "Выбраны варианты, которых нет в вопросе id=" + entry.getKey());
            }
            if (question.getType() == QuestionType.SINGLE_CHOICE && entry.getValue().size() > 1) {
                throw new BusinessRuleException(
                        "В вопросе id=" + entry.getKey() + " можно выбрать только один вариант");
            }
        }
    }
}
