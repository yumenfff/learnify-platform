package com.univer.learnify.integration;

import com.univer.learnify.AbstractIntegrationTest;
import com.univer.learnify.entity.*;
import com.univer.learnify.entity.Module;
import com.univer.learnify.entity.enums.Role;
import com.univer.learnify.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;


@Transactional
class CourseCrudTest extends AbstractIntegrationTest {

    @Autowired
    CourseRepository courseRepository;
    @Autowired
    ModuleRepository moduleRepository;
    @Autowired
    LessonRepository lessonRepository;
    @Autowired
    CategoryRepository categoryRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    TagRepository tagRepository;
    @Autowired
    EnrollmentRepository enrollmentRepository;
    @Autowired
    EntityManager em;

    private Category category;
    private User teacher;

    @BeforeEach
    void setUp() {
        category = categoryRepository.save(new Category("Category " + uid()));
        teacher = userRepository.save(new User("Teacher", uid() + "@test.dev", Role.TEACHER));
    }

    private Course newCourseWithStructure() {
        Course course = new Course("Hibernate", "ORM", category, teacher);
        Module module = new Module("Module 1", 1, null);
        module.addLesson(new Lesson("Lesson 1", 1, "content", null));
        module.addLesson(new Lesson("Lesson 2", 2, "content", null));
        course.addModule(module);
        return course;
    }

    @Test
    void create_cascadesModulesAndLessons() {
        Course saved = courseRepository.saveAndFlush(newCourseWithStructure());
        em.clear();

        Course loaded = courseRepository.findById(saved.getId()).orElseThrow();

        assertEquals(1, loaded.getModules().size());
        assertEquals(2, loaded.getModules().get(0).getLessons().size());
        assertNotNull(loaded.getModules().get(0).getId());
    }

    @Test
    void update_persistsChangedDescription() {
        Course saved = courseRepository.saveAndFlush(newCourseWithStructure());
        saved.setDescription("Новое описание");
        courseRepository.flush();
        em.clear();

        assertEquals("Новое описание", courseRepository.findById(saved.getId()).orElseThrow().getDescription());
    }

    @Test
    void delete_cascadesModulesAndLessons() {
        Course saved = courseRepository.saveAndFlush(newCourseWithStructure());
        Long moduleId = saved.getModules().get(0).getId();
        Long lessonId = saved.getModules().get(0).getLessons().get(0).getId();
        em.clear();

        courseRepository.deleteById(saved.getId());
        courseRepository.flush();

        assertFalse(courseRepository.existsById(saved.getId()));
        assertFalse(moduleRepository.existsById(moduleId));
        assertFalse(lessonRepository.existsById(lessonId));
    }

    @Test
    void manyToMany_tagsAreStoredInJoinTable() {
        Course course = courseRepository.saveAndFlush(newCourseWithStructure());
        Tag tag = tagRepository.save(new Tag("tag-" + uid()));
        course.getTags().add(tag);
        courseRepository.flush();
        em.clear();

        Course loaded = courseRepository.findByIdWithTags(course.getId()).orElseThrow();
        assertEquals(1, loaded.getTags().size());
        assertTrue(courseRepository.findDistinctByTagsName(tag.getName()).stream()
                .anyMatch(c -> c.getId().equals(course.getId())));
    }

    @Test
    void oneToOne_profileIsSavedWithUser() {
        User user = new User("Student", uid() + "@test.dev", Role.STUDENT);
        user.setProfile(new Profile("О себе", "http://avatar"));
        userRepository.saveAndFlush(user);
        em.clear();

        User loaded = userRepository.findById(user.getId()).orElseThrow();
        assertNotNull(loaded.getProfile());
        assertEquals("О себе", loaded.getProfile().getBio());
    }

    @Test
    void uniqueEmail_isEnforcedByDatabase() {
        String email = uid() + "@test.dev";
        userRepository.saveAndFlush(new User("A", email, Role.STUDENT));

        assertThrows(DataIntegrityViolationException.class,
                () -> userRepository.saveAndFlush(new User("B", email, Role.STUDENT)));
    }

    @Test
    void uniqueEnrollment_isEnforcedByDatabase() {
        Course course = courseRepository.saveAndFlush(newCourseWithStructure());
        User student = userRepository.save(new User("Student", uid() + "@test.dev", Role.STUDENT));
        enrollmentRepository.saveAndFlush(new Enrollment(student, course));

        assertThrows(DataIntegrityViolationException.class,
                () -> enrollmentRepository.saveAndFlush(new Enrollment(student, course)));
    }

    private static String uid() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
