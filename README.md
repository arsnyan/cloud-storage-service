# Cloud Storage Service

---

Проект был сделан для roadmap"а Сергея Жукова.

Задеплоенная версия — http://178.236.254.209/

---

# Стэк бэкенда
- **Общее**: Spring Boot 4
- **БД**: PostgreSQL + Spring Data JPA + Liquibase для миграций
- **Аутентификация**: Spring Security + Spring Session Data Redis для хранения сессий
- **Валидация**: Bean Validation
- **S3 хранилище**: MinIO
- **Тестирование**: Testcontainers
- **Дополнительно**: Spring Actuator для health-check в Docker Compose
- **Деплой**: Docker, Docker Compose

---

# Запуск
Для запуска потребуется установить Docker на своё устройство, клонировать проект следующей командой в нужной директории:
```shell
git clone https://github.com/arsnyan/cloud-storage-service.git
```
Для деплоя на удалённом сервере процесс не отличается. Далее используя `cd` зайдите в новую одноимённую с репозиторием директорию и запустите проект:
```shell
docker compose up -d --build
```

Для остановки приложения можно использовать следующую команду:
```shell
docker compose down
```

---

#