# UTMS — University Timetable Management System

## Prerequisites

- Java 17+
- Docker & Docker Compose

## Quick Start

### 1. Start the database

```bash
cd code/utms
docker-compose up -d
```

This starts a PostgreSQL 15 container at `localhost:5432` with:
- Database: `utms`
- Username: `utms`
- Password: `utms`

### 2. Run the application

```bash
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
```

Or if you have Maven installed globally:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### 3. Access the API

- Swagger UI: http://localhost:8080/swagger-ui.html
- Actuator: http://localhost:8080/actuator/health

### 4. Stop the database

```bash
docker-compose down
```

To also remove the data volume:
```bash
docker-compose down -v
```

## Project Structure

```
code/utms/
├── docker-compose.yml          # PostgreSQL for local dev
├── pom.xml                     # Maven build config
├── mvnw.cmd                    # Maven wrapper (Windows)
├── src/
│   ├── main/
│   │   ├── java/com/utms/
│   │   │   ├── UtmsApplication.java
│   │   │   ├── common/         # Shared (config, exceptions, security, DTOs)
│   │   │   └── masterdata/     # Campus, Department, Program, Batch, Section, Course, Faculty, Room
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-local.yml
│   │       └── db/migration/   # Flyway SQL migrations (V1-V8)
│   └── test/
│       └── java/com/utms/      # Unit tests (JUnit 5 + Mockito)
```

## Profiles

| Profile | Usage |
|---------|-------|
| (default) | Production-like config, requires env vars for DB |
| `local` | Local dev with Docker Compose Postgres, debug logging |
