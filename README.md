# Relay

Мессенджер на Kotlin + Spring Boot + PostgreSQL. Реализованы пользователи, личные чаты и группы с ролями (владелец, админ, участник).

## Запуск

```bash
docker compose up -d --build
```

Приложение: `http://localhost:8080`. Настройки (пароль БД, порт, уровень логов) — в `.env`. Миграции (Liquibase) применяются при старте.

Тесты (нужен Docker для Testcontainers):

```bash
./gradlew test
```

## API

Текущий пользователь передаётся в заголовке `X-User-Id`. Ошибки — в формате ProblemDetail.

| Метод | Путь | Что делает |
|---|---|---|
| POST | `/api/users` | регистрация `{"username"}` |
| GET | `/api/users/{id}`, `/api/users?username=` | поиск пользователя |
| PATCH | `/api/users/me/username` | сменить имя |
| GET | `/api/chats`, `/api/chats/{chatId}` | мои чаты, один чат |
| POST | `/api/chats/groups` | создать группу `{"title", "memberIds"}` |
| POST | `/api/chats/direct` | открыть личный чат `{"userId"}` |
| PATCH | `/api/chats/{chatId}/title` | переименовать |
| POST / DELETE | `/api/chats/{chatId}/members[/{userId}]` | добавить / удалить участника |
| PUT | `/api/chats/{chatId}/members/{userId}/role` | сменить роль |
| POST | `/api/chats/{chatId}/owner` | передать владение `{"newOwnerId"}` |
| POST | `/api/chats/{chatId}/leave` | выйти из группы |

## Архитектура

`domain` → `application` (`contracts` — интерфейсы сервисов, `abstractions` — интерфейсы репозиториев) → `infrastructure` (JDBC/PostgreSQL) и `presentation.http` (REST).
