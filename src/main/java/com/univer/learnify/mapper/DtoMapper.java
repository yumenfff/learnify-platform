package com.univer.learnify.mapper;

import com.univer.learnify.dto.response.*;
import com.univer.learnify.entity.*;
import com.univer.learnify.entity.Module;

import java.util.Comparator;
import java.util.List;


public final class DtoMapper {

    private DtoMapper() {
    }

    public static UserResponse toUser(User user) {
        Profile profile = user.getProfile();
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(),
                user.getCreatedAt(),
                profile == null ? null : profile.getBio(),
                profile == null ? null : profile.getAvatarUrl());
    }

    public static CategoryResponse toCategory(Category category) {
        return new CategoryResponse(category.getId(), category.getName());
    }


    public static CourseSummaryResponse toCourseSummary(Course course) {
        return new CourseSummaryResponse(course.getId(), course.getTitle(), course.getDescription(),
                course.getDurationHours(), course.getStartDate(),
                course.getCategory().getId(), course.getCategory().getName(),
                course.getTeacher().getId(), course.getTeacher().getName());
    }


    public static CourseDetailsResponse toCourseDetails(Course course) {
        List<String> tags = course.getTags().stream().map(Tag::getName).sorted().toList();
        List<ModuleResponse> modules = course.getModules().stream()
                .sorted(Comparator.comparing(Module::getOrderIndex))
                .map(DtoMapper::toModule)
                .toList();
        return new CourseDetailsResponse(course.getId(), course.getTitle(), course.getDescription(),
                course.getDurationHours(), course.getStartDate(),
                course.getCategory().getId(), course.getCategory().getName(),
                course.getTeacher().getId(), course.getTeacher().getName(),
                tags, modules);
    }

    public static ModuleResponse toModule(Module module) {
        List<LessonResponse> lessons = module.getLessons().stream()
                .sorted(Comparator.comparing(Lesson::getOrderIndex))
                .map(DtoMapper::toLesson)
                .toList();
        return new ModuleResponse(module.getId(), module.getTitle(), module.getOrderIndex(),
                module.getDescription(), lessons);
    }

    public static LessonResponse toLesson(Lesson lesson) {
        return new LessonResponse(lesson.getId(), lesson.getTitle(), lesson.getOrderIndex(),
                lesson.getContent(), lesson.getVideoUrl());
    }

    public static EnrollmentResponse toEnrollment(Enrollment enrollment) {
        return new EnrollmentResponse(enrollment.getId(), enrollment.getCourse().getId(),
                enrollment.getUser().getId(), enrollment.getEnrollDate(), enrollment.getStatus());
    }

    public static AssignmentResponse toAssignment(Assignment assignment) {
        return new AssignmentResponse(assignment.getId(), assignment.getLesson().getId(),
                assignment.getTitle(), assignment.getDescription(),
                assignment.getDueDate(), assignment.getMaxScore());
    }

    public static SubmissionResponse toSubmission(Submission submission) {
        return new SubmissionResponse(submission.getId(), submission.getAssignment().getId(),
                submission.getStudent().getId(), submission.getSubmittedAt(), submission.getContent(),
                submission.getScore(), submission.getFeedback(), submission.getStatus());
    }


    public static QuizResponse toQuiz(Quiz quiz) {
        List<QuestionResponse> questions = quiz.getQuestions().stream()
                .map(DtoMapper::toQuestion)
                .toList();
        return new QuizResponse(quiz.getId(), quiz.getModule().getId(), quiz.getTitle(),
                quiz.getTimeLimitMinutes(), questions);
    }

    private static QuestionResponse toQuestion(Question question) {
        List<OptionResponse> options = question.getOptions().stream()
                .map(DtoMapper::toOption)
                .toList();
        return new QuestionResponse(question.getId(), question.getText(), question.getType(), options);
    }

    private static OptionResponse toOption(AnswerOption option) {
        return new OptionResponse(option.getId(), option.getText());
    }

    public static QuizResultResponse toQuizResult(QuizSubmission submission) {
        return new QuizResultResponse(submission.getId(), submission.getQuiz().getId(),
                submission.getStudent().getId(), submission.getScore(), submission.getTakenAt());
    }


    public static ReviewResponse toReview(CourseReview review) {
        return new ReviewResponse(review.getId(), review.getCourse().getId(),
                review.getStudent().getId(), review.getStudent().getName(),
                review.getRating(), review.getComment(), review.getCreatedAt());
    }
}
