package com.univer.learnify.service;

import com.univer.learnify.dto.request.CreateCourseRequest;
import com.univer.learnify.dto.request.CreateLessonRequest;
import com.univer.learnify.dto.request.CreateModuleRequest;
import com.univer.learnify.dto.request.UpdateCourseRequest;
import com.univer.learnify.entity.*;
import com.univer.learnify.entity.Module;
import com.univer.learnify.entity.enums.Role;
import com.univer.learnify.exception.BusinessRuleException;
import com.univer.learnify.exception.ConflictException;
import com.univer.learnify.exception.ResourceNotFoundException;
import com.univer.learnify.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;


    @Transactional
    public Course createCourse(CreateCourseRequest request) {
        Category category = findCategory(request.categoryId());
        User teacher = findTeacher(request.teacherId());

        Course course = new Course(request.title(), request.description(), category, teacher);
        course.setDurationHours(request.durationHours());
        course.setStartDate(request.startDate());
        course.setTags(resolveTags(request.tags()));

        if (request.modules() != null) {
            request.modules().forEach(m -> course.addModule(buildModule(m)));
        }

        Course saved = courseRepository.save(course);
        log.info("Создан курс id={}, title={}, teacherId={}", saved.getId(), saved.getTitle(), teacher.getId());
        return saved;
    }


    public Course getCourseDetails(Long id) {
        Course course = courseRepository.findByIdWithModules(id)
                .orElseThrow(() -> new ResourceNotFoundException("Курс", id));

        moduleRepository.findByCourseIdWithLessons(id);
        courseRepository.findByIdWithTags(id);
        return course;
    }


    public List<Course> getCourses(Long categoryId, Long teacherId, String tag, String titlePart) {
        if (categoryId != null) {
            return courseRepository.findByCategoryId(categoryId);
        }
        if (teacherId != null) {
            return courseRepository.findByTeacherId(teacherId);
        }
        if (tag != null && !tag.isBlank()) {
            return courseRepository.findDistinctByTagsName(tag.trim());
        }
        if (titlePart != null && !titlePart.isBlank()) {
            return courseRepository.findByTitleContainingIgnoreCase(titlePart.trim());
        }
        return courseRepository.findAll();
    }

    @Transactional
    public Course updateCourse(Long id, UpdateCourseRequest request) {
        Course course = findCourse(id);
        if (request.title() != null) {
            course.setTitle(request.title());
        }
        if (request.description() != null) {
            course.setDescription(request.description());
        }
        if (request.durationHours() != null) {
            course.setDurationHours(request.durationHours());
        }
        if (request.startDate() != null) {
            course.setStartDate(request.startDate());
        }
        if (request.categoryId() != null) {
            course.setCategory(findCategory(request.categoryId()));
        }
        if (request.teacherId() != null) {
            course.setTeacher(findTeacher(request.teacherId()));
        }
        log.info("Обновлён курс id={}", id);
        return course;
    }


    @Transactional
    public void deleteCourse(Long id) {
        Course course = findCourse(id);
        if (enrollmentRepository.existsByCourseId(id)) {
            throw new ConflictException("Нельзя удалить курс id=" + id + ": на него записаны студенты");
        }
        courseRepository.delete(course);
        log.info("Удалён курс id={}", id);
    }


    @Transactional
    public Module addModule(Long courseId, CreateModuleRequest request) {
        Course course = findCourse(courseId);
        Module module = buildModule(request);
        module.setCourse(course);
        Module saved = moduleRepository.save(module);
        log.info("В курс id={} добавлен модуль id={}", courseId, saved.getId());
        return saved;
    }

    @Transactional
    public Lesson addLesson(Long moduleId, CreateLessonRequest request) {
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Модуль", moduleId));
        Lesson lesson = buildLesson(request);
        lesson.setModule(module);
        Lesson saved = lessonRepository.save(lesson);
        log.info("В модуль id={} добавлен урок id={}", moduleId, saved.getId());
        return saved;
    }

    @Transactional
    public void deleteModule(Long moduleId) {
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Модуль", moduleId));
        moduleRepository.delete(module);
        log.info("Удалён модуль id={}", moduleId);
    }

    @Transactional
    public void deleteLesson(Long lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Урок", lessonId));
        lessonRepository.delete(lesson);
        log.info("Удалён урок id={}", lessonId);
    }


    private Course findCourse(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Курс", id));
    }

    private Category findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Категория", id));
    }

    private User findTeacher(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь", id));
        if (user.getRole() != Role.TEACHER) {
            throw new BusinessRuleException("Пользователь id=" + id + " не является преподавателем");
        }
        return user;
    }

    private Module buildModule(CreateModuleRequest request) {
        Module module = new Module(request.title(), request.orderIndex(), request.description());
        if (request.lessons() != null) {
            request.lessons().forEach(l -> module.addLesson(buildLesson(l)));
        }
        return module;
    }

    private Lesson buildLesson(CreateLessonRequest request) {
        return new Lesson(request.title(), request.orderIndex(), request.content(), request.videoUrl());
    }


    private Set<Tag> resolveTags(Set<String> names) {
        Set<Tag> result = new HashSet<>();
        if (names == null || names.isEmpty()) {
            return result;
        }
        Set<String> normalized = new HashSet<>();
        for (String name : names) {
            String trimmed = name.trim();
            if (!trimmed.isEmpty()) {
                normalized.add(trimmed);
            }
        }
        Map<String, Tag> existing = new HashMap<>();
        tagRepository.findByNameIn(normalized).forEach(t -> existing.put(t.getName(), t));
        for (String name : normalized) {
            Tag tag = existing.get(name);
            if (tag == null) {
                tag = tagRepository.save(new Tag(name));
            }
            result.add(tag);
        }
        return result;
    }
}
