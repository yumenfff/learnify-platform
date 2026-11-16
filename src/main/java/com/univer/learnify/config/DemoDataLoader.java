package com.univer.learnify.config;

import com.univer.learnify.dto.request.*;
import com.univer.learnify.entity.*;
import com.univer.learnify.entity.enums.QuestionType;
import com.univer.learnify.entity.enums.Role;
import com.univer.learnify.repository.UserRepository;
import com.univer.learnify.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Component
@Profile("dev")
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class DemoDataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserService userService;
    private final CategoryService categoryService;
    private final CourseService courseService;
    private final EnrollmentService enrollmentService;
    private final AssignmentService assignmentService;
    private final QuizService quizService;
    private final ReviewService reviewService;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Демо-данные не загружаются: в базе уже есть пользователи");
            return;
        }
        log.info("Загрузка демо-данных...");


        User anna = userService.createUser(new CreateUserRequest(
                "Анна Петрова", "anna.petrova@learnify.dev", Role.TEACHER, "Senior Java-разработчик, 10 лет опыта", null));
        User sergey = userService.createUser(new CreateUserRequest(
                "Сергей Волков", "sergey.volkov@learnify.dev", Role.TEACHER, "Дизайнер интерфейсов", null));
        User ivan = userService.createUser(new CreateUserRequest(
                "Иван Смирнов", "ivan.smirnov@learnify.dev", Role.STUDENT, null, null));
        User maria = userService.createUser(new CreateUserRequest(
                "Мария Кузнецова", "maria.kuznetsova@learnify.dev", Role.STUDENT, "Изучаю backend", null));
        User oleg = userService.createUser(new CreateUserRequest(
                "Олег Иванов", "oleg.ivanov@learnify.dev", Role.STUDENT, null, null));
        userService.createUser(new CreateUserRequest(
                "Администратор", "admin@learnify.dev", Role.ADMIN, null, null));


        Category programming = categoryService.createCategory(new CreateCategoryRequest("Программирование"));
        Category design = categoryService.createCategory(new CreateCategoryRequest("Дизайн"));
        categoryService.createCategory(new CreateCategoryRequest("Маркетинг"));


        Course hibernate = courseService.createCourse(new CreateCourseRequest(
                "Основы Hibernate", "ORM на практике: сущности, связи, ленивая загрузка",
                programming.getId(), anna.getId(), 24, LocalDate.now().plusDays(7),
                Set.of("Java", "Hibernate", "Beginner"),
                List.of(
                        new CreateModuleRequest("Введение в ORM", 1, "Зачем нужен ORM", List.of(
                                new CreateLessonRequest("Что такое ORM", 1, "Объектно-реляционное отображение", null),
                                new CreateLessonRequest("Первая сущность", 2, "Аннотации @Entity и @Id",
                                        "https://example.com/video/entity"))),
                        new CreateModuleRequest("Связи между сущностями", 2, "1-1, 1-N, N-M", List.of(
                                new CreateLessonRequest("OneToMany и ManyToOne", 1, "Владелец связи и mappedBy", null),
                                new CreateLessonRequest("Ленивая загрузка", 2,
                                        "LazyInitializationException и JOIN FETCH", null))))));


        Course figma = courseService.createCourse(new CreateCourseRequest(
                "Figma для начинающих", "Основы проектирования интерфейсов",
                design.getId(), sergey.getId(), 12, LocalDate.now().plusDays(14),
                Set.of("Design", "Beginner"),
                List.of(new CreateModuleRequest("Знакомство с Figma", 1, null, List.of(
                        new CreateLessonRequest("Интерфейс редактора", 1, "Панели и инструменты", null))))));


        enrollmentService.enrollStudent(hibernate.getId(), ivan.getId());
        enrollmentService.enrollStudent(hibernate.getId(), maria.getId());
        enrollmentService.enrollStudent(figma.getId(), maria.getId());
        enrollmentService.enrollStudent(figma.getId(), oleg.getId());


        Long lessonId = courseService.getCourseDetails(hibernate.getId())
                .getModules().get(0).getLessons().get(0).getId();
        Assignment homework = assignmentService.createAssignment(lessonId, new CreateAssignmentRequest(
                "Описать предметную область", "Опишите три сущности и связи между ними",
                Instant.now().plus(7, ChronoUnit.DAYS), 100));
        Submission ivanWork = assignmentService.submitAssignment(homework.getId(),
                new SubmitAssignmentRequest(ivan.getId(), "Курс, Модуль, Урок: курс содержит модули, модуль уроки"));
        assignmentService.submitAssignment(homework.getId(),
                new SubmitAssignmentRequest(maria.getId(), "Пользователь, Заказ, Товар"));
        assignmentService.gradeSubmission(ivanWork.getId(), new GradeSubmissionRequest(90, "Хорошая работа"));


        Long moduleId = courseService.getCourseDetails(hibernate.getId()).getModules().get(0).getId();
        Quiz quiz = quizService.createQuiz(moduleId, new CreateQuizRequest("Тест по введению в ORM", 15, List.of(
                new CreateQuestionRequest("Что означает аббревиатура ORM?", QuestionType.SINGLE_CHOICE, List.of(
                        new CreateOptionRequest("Object-Relational Mapping", true),
                        new CreateOptionRequest("Object Resource Manager", false),
                        new CreateOptionRequest("Online Request Module", false))),
                new CreateQuestionRequest("Какие аннотации относятся к JPA?", QuestionType.MULTIPLE_CHOICE, List.of(
                        new CreateOptionRequest("@Entity", true),
                        new CreateOptionRequest("@Id", true),
                        new CreateOptionRequest("@Autowired", false))))));

        Quiz fullQuiz = quizService.getQuiz(quiz.getId());
        Question q1 = fullQuiz.getQuestions().get(0);
        Question q2 = fullQuiz.getQuestions().get(1);
        Long q1Correct = q1.getOptions().stream().filter(o -> o.isCorrect()).findFirst().orElseThrow().getId();
        Set<Long> q2Correct = q2.getOptions().stream().filter(o -> o.isCorrect())
                .map(o -> o.getId()).collect(java.util.stream.Collectors.toSet());


        quizService.takeQuiz(quiz.getId(), new TakeQuizRequest(ivan.getId(),
                Map.of(q1.getId(), Set.of(q1Correct), q2.getId(), q2Correct)));
        Long q1Wrong = q1.getOptions().stream().filter(o -> !o.isCorrect()).findFirst().orElseThrow().getId();
        quizService.takeQuiz(quiz.getId(), new TakeQuizRequest(maria.getId(),
                Map.of(q1.getId(), Set.of(q1Wrong), q2.getId(), q2Correct)));


        reviewService.createReview(hibernate.getId(), new CreateReviewRequest(ivan.getId(), 5, "Отличный курс"));
        reviewService.createReview(hibernate.getId(), new CreateReviewRequest(maria.getId(), 4, "Много практики"));

        log.info("Демо-данные загружены: пользователей {}, курсов 2", userRepository.count());
    }
}
