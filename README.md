# Learnify Platform

Учебная платформа на **Spring Boot + Hibernate/JPA + PostgreSQL**. Хранит структуру курсов и материалов, пользователей (
студенты, преподаватели, администраторы) и процесс обучения: запись на курс, домашние задания с оценками, тесты, отзывы.

Проект учебный, его цель: показать работу с ORM (связи 1-1, 1-N, M-N, ленивая загрузка и способы её обхода), слоистую
архитектуру и интеграционное тестирование на настоящей БД.

## Стек

| Компонент     | Версия / назначение                       |
|---------------|-------------------------------------------|
| Java          | 21                                        |
| Spring Boot   | 4.1.2 (Web MVC, Data JPA, Validation)     |
| Hibernate ORM | 7.x                                       |
| СУБД          | PostgreSQL 16                             |
| Сборка        | Maven (Maven Wrapper `./mvnw`)            |
| Тесты         | JUnit 5, Mockito, MockMvc, Testcontainers |
| Покрытие      | JaCoCo                                    |
| Контейнеры    | Docker, Docker Compose                    |
| CI            | GitHub Actions                            |
| Прочее        | Lombok                                    |

## Быстрый старт

### Вариант 1: всё в Docker (проще всего)

Нужен только Docker.

```bash
git clone https://github.com/yumenfff/learnify-platform.git
cd learnify-platform
cp .env.example .env          # при желании поменяйте пароль
docker compose up -d --build
```

Приложение: <http://localhost:8080>. При первом запуске (профиль `dev`) база заполняется демо-данными. Проверка:

```bash
curl http://localhost:8080/api/courses
```

Остановка: `docker compose down` (добавьте `-v`, чтобы удалить данные БД).

### Вариант 2: разработка из IntelliJ IDEA / терминала

Нужны JDK 21 и Docker (для PostgreSQL).

```bash
cp .env.example .env
docker compose up -d postgres     # только база данных
./mvnw spring-boot:run            # запускать из корня проекта, чтобы подхватился .env
```

## Конфигурация

Параметры подключения к БД не хранятся в коде. Они читаются из переменных окружения или из файла `.env` в корне
проекта (он в `.gitignore`, в репозитории лежит только `.env.example`).

| Переменная               | Назначение                    | Пример                                      |
|--------------------------|-------------------------------|---------------------------------------------|
| `DB_NAME`                | имя базы (для docker compose) | `learnify`                                  |
| `DB_USER`                | пользователь БД               | `learnify`                                  |
| `DB_PASSWORD`            | пароль БД                     | `change_me`                                 |
| `DB_URL`                 | JDBC URL                      | `jdbc:postgresql://localhost:5432/learnify` |
| `SPRING_PROFILES_ACTIVE` | профиль Spring                | `dev`                                       |

Профили:

| Профиль              | Для чего                      | Особенности                                          |
|----------------------|-------------------------------|------------------------------------------------------|
| `dev` (по умолчанию) | разработка, запуск из коробки | `ddl-auto: update`, SQL в логе, загрузка демо-данных |
| `test`               | автотесты                     | `ddl-auto: create-drop`, БД поднимает Testcontainers |

Отключить демо-данные в профиле `dev`: `app.demo-data.enabled=false`. Схема БД создаётся Hibernate автоматически по
сущностям. Настройка `open-in-view: false` включена намеренно: так `LazyInitializationException` воспроизводится честно,
а данные для ответов загружаются явно.

## Демо-данные

В профиле `dev` при пустой базе `DemoDataLoader` создаёт через сервисный слой:

- 2 преподавателей, 3 студентов, 1 администратора;
- 3 категории, 2 курса с модулями, уроками и тегами;
- записи студентов на курсы;
- задание с двумя решениями (одно оценено);
- тест с вопросами и двумя прохождениями;
- два отзыва.

Загрузка идемпотентна: если пользователи уже есть, повторный запуск ничего не меняет.

## Модель данных

15 сущностей (таблиц 16: плюс `course_tag` для M-N):

```
User ──1─1── Profile
User ──1─N── Course (teacher)          Category ──1─N── Course
Course ──1─N── Module ──1─N── Lesson ──1─N── Assignment ──1─N── Submission ──N─1── User
Course ──M─N── Tag                     (таблица course_tag)
User ──1─N── Enrollment ──N─1── Course (M-N между студентами и курсами с атрибутами)
Module ──1─1── Quiz ──1─N── Question ──1─N── AnswerOption
Quiz ──1─N── QuizSubmission ──N─1── User
Course ──1─N── CourseReview ──N─1── User
```

| Связь | Где реализована                                                                                                   |
|-------|-------------------------------------------------------------------------------------------------------------------|
| 1-1   | `User` ↔ `Profile`, `Module` ↔ `Quiz`                                                                             |
| 1-N   | `Course`→`Module`→`Lesson`→`Assignment`→`Submission`, `Quiz`→`Question`→`AnswerOption`, `Category`→`Course` и др. |
| M-N   | `Course` ↔ `Tag` (таблица `course_tag`), `User` ↔ `Course` через сущность `Enrollment`                            |

Целостность на уровне БД: уникальные `users.email`, `categories.name`, `tags.name`; уникальные пары
`(user_id, course_id)` в записях, `(assignment_id, student_id)` в решениях, `(course_id, student_id)` в отзывах; `CHECK`
на оценку отзыва 1..5; один тест на модуль.

### Ленивая загрузка

Все связи объявлены `fetch = LAZY`, включая `@ManyToOne`. Поэтому обращение к связанным данным вне транзакции даёт
`LazyInitializationException`. Способы, которые используются в проекте:

- `JOIN FETCH` в JPQL (`CourseRepository.findByIdWithModules`, `ModuleRepository.findByCourseIdWithLessons`);
- `@EntityGraph` в методах репозиториев (категория и преподаватель в списках курсов);
- `@Transactional(readOnly = true)` на сервисах и загрузка структуры в несколько запросов в одной транзакции (
  `CourseService.getCourseDetails`). Две коллекции-списка нельзя подтянуть одним `JOIN FETCH` из-за
  `MultipleBagFetchException`;
- ответы API собираются в DTO, а для непроинициализированных связей берётся только `id`.

Особенность Hibernate: сторона `mappedBy` в `@OneToOne` (`User.profile`, `Module.quiz`) не может быть по-настоящему
ленивой, поэтому загружается сразу.

Поведение демонстрирует `LazyLoadingTest`.

### Удаление с зависимостями

| Что удаляем             | Поведение                                                                                     |
|-------------------------|-----------------------------------------------------------------------------------------------|
| Курс                    | каскадно удаляются модули, уроки, задания. **Запрещено (409)**, если есть записанные студенты |
| Модуль, урок            | каскадно удаляются вложенные сущности                                                         |
| Задание                 | запрещено (409), если есть решения                                                            |
| Тест                    | запрещено (409), если есть результаты                                                         |
| Пользователь, категория | если есть связанные записи, БД отвечает нарушением FK, он превращается в 409                  |

## REST API

Базовый путь `/api`. Формат JSON. Идентификаторы `Long`.

| Метод и путь                                                      | Описание                                                                      | Коды               |
|-------------------------------------------------------------------|-------------------------------------------------------------------------------|--------------------|
| `POST /users`                                                     | создать пользователя (роль `STUDENT`/`TEACHER`/`ADMIN`, профиль необязателен) | 201, 400, 409      |
| `GET /users?role=`                                                | список, фильтр по роли                                                        | 200, 400           |
| `GET /users/{id}`                                                 | пользователь                                                                  | 200, 404           |
| `PUT /users/{id}`                                                 | обновить имя и профиль                                                        | 200, 400, 404      |
| `DELETE /users/{id}`                                              | удалить                                                                       | 204, 404, 409      |
| `GET /users/{id}/courses`                                         | курсы студента                                                                | 200, 404           |
| `GET /users/{id}/submissions`                                     | решения студента с оценками                                                   | 200, 404           |
| `GET /users/{id}/quiz-results`                                    | результаты тестов студента                                                    | 200, 404           |
| `POST /categories`, `GET /categories`, `DELETE /categories/{id}`  | категории                                                                     | 201/200/204, 409   |
| `POST /courses`                                                   | создать курс (можно сразу с модулями, уроками и тегами)                       | 201, 400, 404      |
| `GET /courses?categoryId=&teacherId=&tag=&title=`                 | список с фильтрами                                                            | 200                |
| `GET /courses/{id}`                                               | курс со структурой: модули, уроки, теги                                       | 200, 404           |
| `PUT /courses/{id}`                                               | обновить, в том числе сменить преподавателя                                   | 200, 404           |
| `DELETE /courses/{id}`                                            | удалить                                                                       | 204, 404, 409      |
| `POST /courses/{id}/modules`                                      | добавить модуль                                                               | 201, 404           |
| `POST /courses/{id}/enroll?user={userId}`                         | записать студента                                                             | 201, 400, 404, 409 |
| `DELETE /courses/{id}/enroll?user={userId}`                       | отписать                                                                      | 204, 404           |
| `GET /courses/{id}/students`                                      | студенты курса                                                                | 200, 404           |
| `POST /courses/{id}/reviews`, `GET /courses/{id}/reviews`         | отзывы (оценка 1..5, один на студента)                                        | 201/200, 400, 409  |
| `GET /courses/{id}/rating`                                        | средняя оценка                                                                | 200                |
| `GET /courses/{id}/quiz-results`                                  | результаты всех тестов курса                                                  | 200                |
| `POST /modules/{id}/lessons`                                      | добавить урок                                                                 | 201, 404           |
| `DELETE /modules/{id}`                                            | удалить модуль                                                                | 204, 404           |
| `POST /modules/{id}/quiz`, `GET /modules/{id}/quiz`               | создать и получить тест модуля                                                | 201/200, 400, 409  |
| `DELETE /lessons/{id}`                                            | удалить урок                                                                  | 204, 404           |
| `POST /lessons/{id}/assignments`, `GET /lessons/{id}/assignments` | задания урока                                                                 | 201/200, 404       |
| `GET /assignments/{id}`, `DELETE /assignments/{id}`               | задание                                                                       | 200/204, 404, 409  |
| `POST /assignments/{id}/submit`                                   | отправить решение                                                             | 201, 400, 404, 409 |
| `GET /assignments/{id}/submissions`                               | решения по заданию                                                            | 200, 404           |
| `PUT /submissions/{id}/grade`                                     | выставить оценку                                                              | 200, 400, 404      |
| `GET /quizzes/{id}`, `DELETE /quizzes/{id}`                       | тест (без отметок верных ответов)                                             | 200/204, 404, 409  |
| `POST /quizzes/{id}/take`                                         | пройти тест                                                                   | 201, 400, 404      |
| `GET /quizzes/{id}/results`                                       | результаты по тесту                                                           | 200, 404           |
| `DELETE /reviews/{id}`                                            | удалить отзыв                                                                 | 204, 404           |

### Бизнес-правила

- Курс ведёт только пользователь с ролью `TEACHER`, записаться и сдавать работы может только `STUDENT`.
- Повторная запись на курс, повторная сдача задания и повторный отзыв дают 409 (проверка в сервисе плюс уникальные
  ограничения БД).
- Решение принимается только от студента, записанного на курс, и до дедлайна.
- Оценка не может быть больше `maxScore` задания.
- Тест: один на модуль. В вопросе минимум 2 варианта и хотя бы один верный, при `SINGLE_CHOICE` верный ровно один.
  Вопрос засчитывается, только если выбранные варианты точно совпадают с набором верных. Результат это процент верных
  ответов, повторные попытки разрешены.

### Формат ошибок

Все ошибки обрабатывает `GlobalExceptionHandler` (`@RestControllerAdvice`):

```json
{
  "timestamp": "2026-11-10T12:42:28.815Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Ошибка валидации входных данных",
  "fieldErrors": {
    "email": "должно иметь формат адреса электронной почты"
  }
}
```

### Пример сценария

```bash
H='Content-Type: application/json'; B=http://localhost:8080/api

curl -X POST $B/users -H "$H" -d '{"name":"Анна","email":"anna@example.com","role":"TEACHER"}'
curl -X POST $B/users -H "$H" -d '{"name":"Иван","email":"ivan@example.com","role":"STUDENT"}'
curl -X POST $B/categories -H "$H" -d '{"name":"Программирование"}'
curl -X POST $B/courses -H "$H" -d '{"title":"Основы Hibernate","categoryId":1,"teacherId":1,
  "tags":["Java"],"modules":[{"title":"Введение","orderIndex":1,
  "lessons":[{"title":"Что такое ORM","orderIndex":1}]}]}'
curl "$B/courses/1/enroll?user=2" -X POST
curl -X POST $B/lessons/1/assignments -H "$H" -d '{"title":"ДЗ 1","maxScore":100}'
curl -X POST $B/assignments/1/submit -H "$H" -d '{"studentId":2,"content":"Моё решение"}'
curl -X PUT $B/submissions/1/grade -H "$H" -d '{"score":90,"feedback":"Хорошо"}'
```

(Идентификаторы в примере подразумевают пустую БД. Если включены демо-данные, используйте свои.)

## Тестирование

```bash
./mvnw clean verify     # все тесты и отчёт о покрытии
```

Нужен запущенный Docker: интеграционные тесты поднимают настоящий PostgreSQL через Testcontainers, один общий контейнер
на весь прогон. Отчёт о покрытии: `target/site/jacoco/index.html`.

| Группа              | Класс                                                                                             | Что проверяет                                                                               |
|---------------------|---------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------|
| Интеграционные CRUD | `CourseCrudTest`                                                                                  | каскадное создание и удаление, обновление, M-N теги, 1-1 профиль, уникальные ограничения БД |
| Ленивая загрузка    | `LazyLoadingTest`                                                                                 | `LazyInitializationException` и способы её обойти (JOIN FETCH, транзакционный сервис)       |
| Сценарии сервисов   | `ServiceFlowTest`                                                                                 | запись и отписка, удаление курса, задания, тесты, отзывы на настоящей БД                    |
| REST API            | `ApiControllerTest`                                                                               | коды ответов, формат ошибок, сквозные сценарии через HTTP (MockMvc)                         |
| Юнит-тесты          | `EnrollmentServiceTest`, `AssignmentServiceTest`, `QuizServiceTest`, `GlobalExceptionHandlerTest` | бизнес-правила с моками, без БД                                                             |

## Архитектура

```
src/main/java/com/univer/learnify/
├── config/        DemoDataLoader (демо-данные для профиля dev)
├── controller/    REST-контроллеры (тонкие, только HTTP)
├── dto/
│   ├── request/   входные DTO с валидацией (Jakarta Validation)
│   └── response/  выходные DTO (records)
├── entity/        JPA-сущности, entity/enums, перечисления
├── exception/     свои исключения и GlobalExceptionHandler
├── mapper/        DtoMapper: сущность в DTO
├── repository/    Spring Data JPA (JOIN FETCH, @EntityGraph)
└── service/       бизнес-логика и транзакции
```

Принципы: контроллер ничего не знает о JPA и работает с DTO, вся логика и границы транзакций в сервисах, зависимости
внедряются через конструктор, сущности не отдаются наружу (на них нет `@Data`, у них не переопределены `equals`/
`hashCode`/`toString`, чтобы не трогать ленивые коллекции).

## CI/CD

GitHub Actions (`.github/workflows/ci.yml`) на каждый push в `main` и каждый pull request:

1. собирает проект и запускает все тесты (`./mvnw verify`);
2. публикует отчёт JaCoCo артефактом;
3. проверяет, что собирается Docker-образ.

`Dockerfile` использует многоступенчатую сборку, итоговый образ запускается под непривилегированным пользователем.
