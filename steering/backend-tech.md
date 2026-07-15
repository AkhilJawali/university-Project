---
inclusion: auto
name: backend-tech
description: Backend technology stack and code-generation conventions for Java / Spring Boot. Use whenever designing, scaffolding, or writing backend code, Spring Boot services, REST APIs, JPA entities, or Java build files (pom.xml / build.gradle).
---

# Backend Tech Standards (Java / Spring Boot)

These conventions govern how backend code is designed and generated. Follow them
during both design derivation and code development.

## Precondition — Design must be Approved

Code development starts **only** when the design is approved.

- Before writing or scaffolding any application code, locate the story's
  **Requirement Design Derivation** task and check its status via the Atlassian MCP.
- **If the status is not `Approved`**, stop and tell the user:
  > "Development can't start yet — the Requirement Design Derivation task
  > {DESIGN-KEY} is currently '{status}', not 'Approved'. Please get the design
  > approved first."
  Do not generate code.
- Only when it is `Approved`, proceed with development.

## Stack

- **Language:** Java **21 (LTS)**. Use Java 21 for this project — do not use a
  different major version.
- **Framework:** Spring Boot, **latest stable GA** (4.1.x line as of mid-2026,
  built on Spring Framework 7, Java 17+ baseline).
- **Build tool:** <choose one — Maven or Gradle> and keep it consistent.
- **Persistence:** Spring Data JPA with a real migration tool (Flyway or
  Liquibase). Never auto-generate/alter schema in production (`ddl-auto` must
  not be `update`/`create` outside local/test).
- **API docs:** springdoc-openapi (generates OpenAPI 3 / Swagger UI).
- **Testing:** JUnit 5 + Spring Boot Test; Testcontainers for integration tests.
- **Security:** Spring Security.

> IMPORTANT: version numbers move. **Verify the latest GA at scaffold time**
> (e.g. via start.spring.io / Spring Initializr or Maven Central) rather than
> trusting a hardcoded number here. For a new app, always start from the current
> stable release.

## New application scaffolding

- Generate the project via Spring Initializr conventions (latest Spring Boot GA
  + Java 21).
- Pick starters based on the design: `web` (or `webflux` if reactive), `data-jpa`,
  `validation`, `security`, `actuator`, the DB driver, and `springdoc`.
- Set the Java version and Spring Boot version explicitly in the build file.

## Project & package structure

Organize by feature, then by layer within each feature:

```
com.technoboost.payments
  ├── <feature>
  │   ├── api          # @RestController + request/response DTOs
  │   ├── service      # business logic
  │   ├── domain       # entities, value objects
  │   ├── repository   # Spring Data repositories
  │   └── mapper       # DTO <-> entity mapping
  ├── config           # @Configuration, security, beans
  └── common           # shared utilities, error handling
```

## Coding conventions

- **Layering:** controllers stay thin; business logic lives in `@Service`;
  data access in repositories. Do not put logic in controllers.
- **DTOs:** expose DTOs from controllers — **never** expose JPA entities directly.
  Prefer Java `record` for immutable DTOs.
- **Dependency injection:** constructor injection only. No field injection
  (`@Autowired` on fields is not allowed).
- **REST APIs:** version paths (`/api/v1/...`); use nouns for resources; use
  correct HTTP status codes; validate input with Jakarta Validation
  (`@Valid`, constraint annotations).
- **Error handling:** centralize with `@RestControllerAdvice`; return
  structured errors (RFC 7807 Problem Details) — no stack traces to clients.
- **Configuration:** use `application.yml` with Spring profiles. **Never**
  hardcode secrets/credentials — use environment variables or a secrets manager.
- **Persistence:** repositories via Spring Data; schema changes via
  Flyway/Liquibase migrations checked into the repo.
- **Nullability & immutability:** prefer immutable objects; avoid returning null
  collections (return empty).
- **Logging:** SLF4J; no `System.out.println`; no logging of secrets/PII.

## Testing (supports the Unit Test sub-task)

- Unit-test services and business logic with JUnit 5 + Mockito.
- Integration-test the web/persistence layers with `@SpringBootTest` and
  Testcontainers for real DB behavior.
- **Unit test code coverage must be above 90%.** Enforce it in the build with
  JaCoCo (`jacocoTestCoverageVerification`) so the build fails below the
  threshold. Coverage must be meaningful — cover real branches and edge cases,
  not trivial getters padded to hit the number.
- New logic ships with tests.

## API documentation (supports the API Documentation sub-task)

- Annotate controllers/DTOs so springdoc produces an accurate OpenAPI spec.
- Keep the generated OpenAPI/Swagger docs in sync with the implemented endpoints.

## Do NOT

- Do not use field injection.
- Do not expose entities in API responses.
- Do not use `ddl-auto: update`/`create` outside local/test.
- Do not hardcode secrets or environment-specific values.
- Do not add heavy dependencies when Spring Boot starters already cover the need.
- Do not use a Java version other than 21 for this project.
- Do not merge code with unit test coverage at or below 90%.

> Choose the build tool (Maven or Gradle) and keep it consistent across the project.
