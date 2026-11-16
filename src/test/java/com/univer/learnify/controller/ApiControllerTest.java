package com.univer.learnify.controller;

import com.jayway.jsonpath.JsonPath;
import com.univer.learnify.AbstractIntegrationTest;
import com.univer.learnify.entity.Course;
import com.univer.learnify.entity.User;
import com.univer.learnify.service.EnrollmentService;
import com.univer.learnify.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


class ApiControllerTest extends AbstractIntegrationTest {

    @Autowired
    WebApplicationContext context;
    @Autowired
    TestDataFactory factory;
    @Autowired
    EnrollmentService enrollmentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }


    @Test
    void createUser_valid_returns201() throws Exception {
        String email = "user-" + uid() + "@test.dev";

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test User\",\"email\":\"" + email + "\",\"role\":\"STUDENT\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void createUser_invalid_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"not-an-email\",\"role\":\"STUDENT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    void createUser_duplicateEmail_returns409() throws Exception {
        String body = "{\"name\":\"Dup\",\"email\":\"dup-" + uid() + "@test.dev\",\"role\":\"STUDENT\"}";

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void getUser_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/users/{id}", 999_999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void listUsers_unknownRole_returns400() throws Exception {
        mockMvc.perform(get("/api/users").param("role", "BOSS"))
                .andExpect(status().isBadRequest());
    }


    @Test
    void createCourse_returns201WithStructure() throws Exception {
        User teacher = factory.createTeacher();
        Long categoryId = factory.createCategory().getId();
        String body = """
                {"title":"API course","categoryId":%d,"teacherId":%d,"tags":["java"],
                 "modules":[{"title":"M1","orderIndex":1,
                   "lessons":[{"title":"L1","orderIndex":1},{"title":"L2","orderIndex":2}]}]}
                """.formatted(categoryId, teacher.getId());

        mockMvc.perform(post("/api/courses").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.teacherId").value(teacher.getId()))
                .andExpect(jsonPath("$.tags[0]").value("java"))
                .andExpect(jsonPath("$.modules.length()").value(1))
                .andExpect(jsonPath("$.modules[0].lessons.length()").value(2));
    }

    @Test
    void getCourse_returnsFullStructureWithoutLazyErrors() throws Exception {
        Course course = factory.createCourse(factory.createTeacher());

        mockMvc.perform(get("/api/courses/{id}", course.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modules[0].lessons.length()").value(2))
                .andExpect(jsonPath("$.categoryName").isString())
                .andExpect(jsonPath("$.teacherName").isString());
    }

    @Test
    void listCourses_filterByTeacher_returnsOnlyTheirCourses() throws Exception {
        User teacher = factory.createTeacher();
        Course course = factory.createCourse(teacher);

        mockMvc.perform(get("/api/courses").param("teacherId", teacher.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(course.getId()));
    }


    @Test
    void enrollment_flow_enrollDuplicateTeacherAndUnenroll() throws Exception {
        User teacher = factory.createTeacher();
        User student = factory.createStudent();
        Course course = factory.createCourse(teacher);
        String user = student.getId().toString();

        mockMvc.perform(post("/api/courses/{id}/enroll", course.getId()).param("user", user))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        mockMvc.perform(post("/api/courses/{id}/enroll", course.getId()).param("user", user))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/courses/{id}/enroll", course.getId())
                        .param("user", teacher.getId().toString()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/courses/{id}/enroll", course.getId()))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/courses/{id}/students", course.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/users/{id}/courses", student.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(course.getId()));

        mockMvc.perform(delete("/api/courses/{id}/enroll", course.getId()).param("user", user))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteCourse_withEnrollment_returns409_thenSucceedsAfterUnenroll() throws Exception {
        User student = factory.createStudent();
        Course course = factory.createCourse(factory.createTeacher());
        enrollmentService.enrollStudent(course.getId(), student.getId());

        mockMvc.perform(delete("/api/courses/{id}", course.getId()))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/courses/{id}/enroll", course.getId())
                        .param("user", student.getId().toString()))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/courses/{id}", course.getId()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/courses/{id}", course.getId()))
                .andExpect(status().isNotFound());
    }


    @Test
    void assignment_flow_submitDuplicateAndGrade() throws Exception {
        User student = factory.createStudent();
        Course course = factory.createCourse(factory.createTeacher());
        enrollmentService.enrollStudent(course.getId(), student.getId());

        MvcResult assignment = mockMvc.perform(post("/api/lessons/{id}/assignments", factory.firstLessonId(course))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"HW\",\"maxScore\":100}"))
                .andExpect(status().isCreated())
                .andReturn();
        long assignmentId = number(assignment, "$.id");

        String submitBody = "{\"studentId\":" + student.getId() + ",\"content\":\"solution\"}";
        MvcResult submission = mockMvc.perform(post("/api/assignments/{id}/submit", assignmentId)
                        .contentType(MediaType.APPLICATION_JSON).content(submitBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andReturn();
        long submissionId = number(submission, "$.id");

        mockMvc.perform(post("/api/assignments/{id}/submit", assignmentId)
                        .contentType(MediaType.APPLICATION_JSON).content(submitBody))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/api/submissions/{id}/grade", submissionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":90,\"feedback\":\"ok\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("GRADED"))
                .andExpect(jsonPath("$.score").value(90));
        mockMvc.perform(put("/api/submissions/{id}/grade", submissionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":101}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/users/{id}/submissions", student.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].score").value(90));
        mockMvc.perform(get("/api/assignments/{id}/submissions", assignmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }


    @Test
    void quiz_flow_createHidesCorrectFlagAndTakeScores() throws Exception {
        User student = factory.createStudent();
        Course course = factory.createCourse(factory.createTeacher());
        enrollmentService.enrollStudent(course.getId(), student.getId());
        Long moduleId = factory.firstModuleId(course);

        String quizBody = """
                {"title":"Quiz","questions":[{"text":"Q?","type":"SINGLE_CHOICE","options":[
                  {"text":"yes","correct":true},{"text":"no","correct":false}]}]}
                """;
        MvcResult quiz = mockMvc.perform(post("/api/modules/{id}/quiz", moduleId)
                        .contentType(MediaType.APPLICATION_JSON).content(quizBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.questions[0].options.length()").value(2))
                .andExpect(jsonPath("$.questions[0].options[0].correct").doesNotExist())
                .andReturn();
        long quizId = number(quiz, "$.id");
        long questionId = number(quiz, "$.questions[0].id");
        long correctOptionId = number(quiz, "$.questions[0].options[0].id");

        mockMvc.perform(post("/api/modules/{id}/quiz", moduleId)
                        .contentType(MediaType.APPLICATION_JSON).content(quizBody))
                .andExpect(status().isConflict());

        String takeBody = "{\"studentId\":" + student.getId()
                + ",\"answers\":{\"" + questionId + "\":[" + correctOptionId + "]}}";
        mockMvc.perform(post("/api/quizzes/{id}/take", quizId)
                        .contentType(MediaType.APPLICATION_JSON).content(takeBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.score").value(100));

        mockMvc.perform(get("/api/users/{id}/quiz-results", student.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].score").value(100));
        mockMvc.perform(get("/api/courses/{id}/quiz-results", course.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }


    @Test
    void review_flow_createValidateAndAverageRating() throws Exception {
        User student = factory.createStudent();
        Course course = factory.createCourse(factory.createTeacher());
        enrollmentService.enrollStudent(course.getId(), student.getId());

        mockMvc.perform(post("/api/courses/{id}/reviews", course.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":" + student.getId() + ",\"rating\":6}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.rating").exists());

        mockMvc.perform(post("/api/courses/{id}/reviews", course.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":" + student.getId() + ",\"rating\":5,\"comment\":\"great\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(5));

        mockMvc.perform(get("/api/courses/{id}/rating", course.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.average").value(5.0));
        mockMvc.perform(get("/api/courses/{id}/reviews", course.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }


    private static long number(MvcResult result, String path) throws Exception {
        Number value = JsonPath.read(result.getResponse().getContentAsString(), path);
        return value.longValue();
    }

    private static String uid() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
