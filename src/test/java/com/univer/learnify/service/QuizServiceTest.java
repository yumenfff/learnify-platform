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
import com.univer.learnify.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock
    QuizRepository quizRepository;
    @Mock
    QuestionRepository questionRepository;
    @Mock
    QuizSubmissionRepository quizSubmissionRepository;
    @Mock
    ModuleRepository moduleRepository;
    @Mock
    CourseRepository courseRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    EnrollmentRepository enrollmentRepository;
    @InjectMocks
    QuizService service;

    private User student;


    @BeforeEach
    void setUp() {
        Course course = new Course();
        course.setId(10L);
        Module module = new Module();
        module.setCourse(course);
        Quiz quiz = new Quiz("Quiz", null);
        quiz.setId(1L);
        quiz.setModule(module);

        student = new User("Student", "s@test.dev", Role.STUDENT);
        student.setId(3L);

        Question q1 = question(1L, QuestionType.SINGLE_CHOICE, option(11L, true), option(12L, false));
        Question q2 = question(2L, QuestionType.MULTIPLE_CHOICE,
                option(21L, true), option(22L, true), option(23L, false));

        lenient().when(quizRepository.findById(1L)).thenReturn(Optional.of(quiz));
        lenient().when(userRepository.findById(3L)).thenReturn(Optional.of(student));
        lenient().when(enrollmentRepository.existsByUserIdAndCourseId(3L, 10L)).thenReturn(true);
        lenient().when(questionRepository.findByQuizIdWithOptions(1L)).thenReturn(List.of(q1, q2));
        lenient().when(quizSubmissionRepository.save(any(QuizSubmission.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void takeQuiz_allAnswersCorrect_scoreIs100() {
        QuizSubmission result = service.takeQuiz(1L, new TakeQuizRequest(3L,
                Map.of(1L, Set.of(11L), 2L, Set.of(21L, 22L))));

        assertEquals(100, result.getScore());
    }

    @Test
    void takeQuiz_incompleteMultipleChoice_isNotCounted() {
        QuizSubmission result = service.takeQuiz(1L, new TakeQuizRequest(3L,
                Map.of(1L, Set.of(11L), 2L, Set.of(21L))));

        assertEquals(50, result.getScore());
    }

    @Test
    void takeQuiz_optionFromAnotherQuestion_throwsBusinessRule() {
        assertThrows(BusinessRuleException.class, () -> service.takeQuiz(1L,
                new TakeQuizRequest(3L, Map.of(1L, Set.of(99L)))));
    }

    @Test
    void createQuiz_singleChoiceWithTwoCorrectOptions_throwsBusinessRule() {
        Module module = new Module();
        when(moduleRepository.findById(5L)).thenReturn(Optional.of(module));
        when(quizRepository.existsByModuleId(5L)).thenReturn(false);

        CreateQuizRequest request = new CreateQuizRequest("Quiz", null, List.of(
                new CreateQuestionRequest("Q", QuestionType.SINGLE_CHOICE, List.of(
                        new CreateOptionRequest("a", true),
                        new CreateOptionRequest("b", true)))));

        assertThrows(BusinessRuleException.class, () -> service.createQuiz(5L, request));
    }

    private static Question question(long id, QuestionType type, AnswerOption... options) {
        Question question = new Question("Вопрос " + id, type);
        question.setId(id);
        for (AnswerOption option : options) {
            question.addOption(option);
        }
        return question;
    }

    private static AnswerOption option(long id, boolean correct) {
        AnswerOption option = new AnswerOption("Вариант " + id, correct);
        option.setId(id);
        return option;
    }
}
