package com.univer.learnify.support;

import com.univer.learnify.dto.request.*;
import com.univer.learnify.entity.Category;
import com.univer.learnify.entity.Course;
import com.univer.learnify.entity.User;
import com.univer.learnify.entity.enums.Role;
import com.univer.learnify.service.CategoryService;
import com.univer.learnify.service.CourseService;
import com.univer.learnify.service.UserService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;


@Component
public class TestDataFactory {

    private final UserService userService;
    private final CategoryService categoryService;
    private final CourseService courseService;

    public TestDataFactory(UserService userService, CategoryService categoryService, CourseService courseService) {
        this.userService = userService;
        this.categoryService = categoryService;
        this.courseService = courseService;
    }

    public User createTeacher() {
        return createUser(Role.TEACHER);
    }

    public User createStudent() {
        return createUser(Role.STUDENT);
    }

    public Category createCategory() {
        return categoryService.createCategory(new CreateCategoryRequest("Category " + suffix()));
    }


    public Course createCourse(User teacher) {
        Category category = createCategory();
        return courseService.createCourse(new CreateCourseRequest(
                "Course " + suffix(), "Description", category.getId(), teacher.getId(),
                10, LocalDate.now().plusDays(1), Set.of("test"),
                List.of(new CreateModuleRequest("Module 1", 1, null, List.of(
                        new CreateLessonRequest("Lesson 1", 1, "content", null),
                        new CreateLessonRequest("Lesson 2", 2, "content", null))))));
    }

    public Long firstModuleId(Course course) {
        return courseService.getCourseDetails(course.getId()).getModules().get(0).getId();
    }

    public Long firstLessonId(Course course) {
        return courseService.getCourseDetails(course.getId()).getModules().get(0).getLessons().get(0).getId();
    }

    private User createUser(Role role) {
        String s = suffix();
        return userService.createUser(new CreateUserRequest(
                role.name() + " " + s, role.name().toLowerCase() + "-" + s + "@test.dev", role, null, null));
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
