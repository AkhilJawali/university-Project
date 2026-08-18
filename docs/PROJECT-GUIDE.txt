# UTMS Project Guide — How It All Works

This document explains the complete project setup, structure, rules, and workflow so that any beginner can understand how Kiro (AI assistant) works alongside the development team to build software from start to finish.

---

## 1. Project Structure

```
AIDLC/                              ← Root workspace
├── .kiro/                          ← Kiro configuration (the AI brain)
│   ├── steering/                   ← Rules & standards the AI follows
│   │   ├── workflow.md             ← Development flow, Jira rules, gating logic
│   │   ├── brd-to-jira.md         ← How BRD → Epic → Stories → Subtasks
│   │   ├── design-derivation.md   ← How to generate design docs from requirements
│   │   ├── requirements-generation.md ← How to create requirement docs
│   │   ├── squad/
│   │   │   ├── squad-rules.md     ← Team agreements, code review, AI review process
│   │   │   ├── tech.md            ← Tech stack (Java, Spring Boot, React, Postgres)
│   │   │   ├── product.md         ← Product context (what we're building)
│   │   │   └── structure.md       ← Folder layout conventions
│   │   ├── shared/
│   │   │   ├── jira.md            ← Jira project config, issue types, API notes
│   │   │   ├── git.md             ← Branch strategy, commit format
│   │   │   ├── testing.md         ← Test pyramid, tools, commands
│   │   │   └── mcp.md             ← MCP server configuration
│   │   └── role/
│   │       ├── backend/
│   │       │   ├── backend-standards.md  ← Java/Spring coding conventions
│   │       │   ├── api-standards.md      ← REST API design rules
│   │       │   └── data-access.md        ← JPA, Flyway, DB conventions
│   │       └── frontend/
│   │           ├── frontend-standards.md ← React/JSX coding conventions
│   │           ├── ui-standards.md       ← UI design system rules
│   │           └── state-management.md   ← TanStack Query + Zustand patterns
│   ├── hooks/
│   │   └── auto-code-review.json  ← Hook that tracks file changes for review
│   └── settings/
│       └── mcp.json               ← API keys for Jira/GitHub (gitignored)
│
├── code/                           ← All source code lives here
│   ├── utms/                       ← Backend (Spring Boot + Java)
│   │   ├── pom.xml                ← Maven build config with all dependencies
│   │   ├── src/main/java/com/utms/
│   │   │   ├── UtmsApplication.java      ← App entry point
│   │   │   ├── common/                   ← Shared utilities
│   │   │   │   ├── config/               ← JPA auditing, app config
│   │   │   │   ├── dto/                  ← PagedResponse, CreateResponse
│   │   │   │   ├── entity/BaseEntity.java ← Base class all entities extend
│   │   │   │   ├── exception/            ← Global error handler + custom exceptions
│   │   │   │   ├── mapper/BaseMapperConfig.java ← MapStruct config (ignore BaseEntity fields)
│   │   │   │   └── security/             ← SecurityConfig, RLS filter, UserContext
│   │   │   └── masterdata/               ← Business modules
│   │   │       ├── campus/               ← Campus CRUD (entity, service, controller, repo, DTOs)
│   │   │       ├── department/           ← Department CRUD
│   │   │       ├── program/              ← Program CRUD
│   │   │       ├── batch/                ← Batch CRUD
│   │   │       ├── section/              ← Section CRUD
│   │   │       ├── course/               ← Course CRUD (L-T-P, prerequisites)
│   │   │       ├── faculty/              ← Faculty CRUD (availability, competencies)
│   │   │       ├── room/                 ← Room CRUD (resource blocks)
│   │   │       ├── academiccalendar/     ← Calendar, holidays, exam windows
│   │   │       └── timeslot/             ← Time-slot grids, slot definitions, working days
│   │   ├── src/main/resources/
│   │   │   ├── application.yml           ← Main config (defaults to local profile)
│   │   │   ├── application-local.yml     ← Local dev config (Postgres credentials)
│   │   │   └── db/migration/            ← Flyway SQL migrations (V1 through V10)
│   │   │       └── seed/                ← Seed data for local dev (run manually)
│   │   └── src/test/java/               ← Unit tests (JUnit 5 + Mockito)
│   │
│   └── frontend/                   ← Frontend (React + JavaScript)
│       ├── package.json            ← Dependencies (React, TanStack Query, Zod, etc.)
│       ├── vite.config.js          ← Vite dev server + API proxy to backend
│       ├── tailwind.config.js      ← Tailwind CSS config
│       ├── index.html              ← App entry HTML
│       └── src/
│           ├── main.jsx            ← App root (QueryClient, Router, ErrorBoundary)
│           ├── index.css           ← Tailwind imports
│           ├── api/                ← Typed API client modules (campusApi, calendarApi, etc.)
│           ├── components/         ← Shared UI (LoadingSkeleton, ErrorState, Dialogs)
│           ├── features/           ← Feature modules
│           │   ├── master-data/    ← Campus, Department, Program, Batch, Section pages
│           │   └── scheduling-config/ ← Calendar, Grid, Timeline pages
│           ├── hooks/              ← Custom hooks (useDebounce)
│           ├── layouts/            ← AdminLayout (sidebar + content)
│           ├── routes/             ← React Router route definitions
│           └── stores/             ← Zustand stores (UI state)
│
├── docs/                           ← All documentation
│   ├── requirements/              ← Requirement docs (one per story)
│   ├── design/                    ← Design docs (one per story)
│   ├── testing/                   ← Unit test result docs
│   ├── code-coverage/             ← Coverage report docs
│   ├── code-review/               ← AI review findings
│   └── PROJECT-GUIDE.md           ← This file
│
└── semantic-review/               ← AI code review raw outputs
```

---

## 2. Basic Rules

### Steering File Hierarchy

Kiro's rules are organized in layers. Higher levels apply to everything; lower levels are more specific:

```
Organization Level (applies to ALL projects in the org)
├── Security standards (input validation, XSS prevention, parameterized queries)
├── Clean code principles (single responsibility, composition over inheritance)
├── Documentation standards (every API documented, ADRs required)
├── Collaboration rules (PRs required, conventional commits)
│
└── Squad Level (applies to this team's projects)
    ├── Tech stack (Java 21, Spring Boot, React, Postgres)
    ├── Working agreements (sprint ceremonies, review standards, DoD)
    ├── Product context (what we're building, personas, metrics)
    ├── Workflow rules (gating, Jira statuses, AI review process)
    │
    └── Role Level (applies to specific role — backend or frontend)
        ├── Backend standards (package structure, layering, naming)
        ├── Frontend standards (component structure, state management)
        ├── API standards (URL conventions, status codes, DTOs)
        ├── Data access standards (JPA, Flyway, transactions)
        │
        └── Task Level (applies to specific story/ticket)
            └── Dev subtasks, requirement docs, design docs
```

**Precedence:** If a squad rule conflicts with an org rule, the squad rule wins. If a role rule conflicts with a squad rule, the role rule wins for that specific context.

**Location in project:**
| Level | Path |
|-------|------|
| Organization | `.kiro/steering/squad/squad-rules.md` (working agreements, security) |
| Squad | `.kiro/steering/squad/tech.md`, `product.md`, `structure.md` |
| Role — Backend | `.kiro/steering/role/backend/backend-standards.md`, `api-standards.md`, `data-access.md` |
| Role — Frontend | `.kiro/steering/role/frontend/frontend-standards.md`, `ui-standards.md`, `state-management.md` |
| Shared (cross-role) | `.kiro/steering/shared/jira.md`, `git.md`, `testing.md`, `mcp.md` |
| Workflow | `.kiro/steering/workflow.md` (the main flow everyone follows) |

### Tech Stack
| Layer | Technology |
|-------|-----------|
| Backend | Java 21 (LTS), Spring Boot 3.4.x, Maven |
| Frontend | React 18+, plain JavaScript/JSX (NO TypeScript) |
| Database | PostgreSQL 15+, Flyway migrations |
| State (Frontend) | TanStack Query (server), Zustand (client) |
| Validation | Jakarta Validation (backend), Zod (frontend) |
| Testing | JUnit 5 + Mockito (backend), Vitest + RTL (frontend) |

### Golden Rules (Non-Negotiable)

1. **No TypeScript** — Frontend is plain JavaScript/JSX only. Never create .ts or .tsx files.
2. **No code on main** — All work happens on feature branches. Main is always stable.
3. **Build before push** — Backend must produce a clean package, frontend must build without errors, all tests must pass.
4. **Story assignment = permission** — You can only work on stories assigned to you.
5. **Gating is mandatory** — Requirements must be approved before design. Design must be approved before code. Code review must pass before testing.
6. **Every table has BaseEntity columns** — id, is_active, created_at, updated_at, created_by, updated_by, deleted_at.
7. **All mappers use BaseMapperConfig** — Prevents "unmapped target" compile errors.
8. **Subtasks use parent numeric ID** — When creating Jira subtasks via API, use `parent: { id: "12345" }` not key.
9. **Documents are .md files** — Stored locally in `docs/` AND attached to Jira subtasks.
10. **AI review before human review** — The semantic_reviewer runs during the Code Review step, fixes blocking issues, then assigns to human.

### Jira Hierarchy
```
Epic (one per project/BRD)
└── Story (≤ 8 story points, testable)
    ├── Subtask: Requirement Generation
    ├── Subtask: Requirement Design Derivation
    ├── Subtask: Code Review
    ├── Subtask: Testing
    │
    │   (Added after design approval:)
    ├── Subtask: Dev Task 1
    ├── Subtask: Dev Task 2
    ├── Subtask: Unit Tests
    └── Subtask: Code Coverage
```

### Every Backend Story Has a Frontend Counterpart
| Backend Story | Frontend Story |
|---------------|----------------|
| AID-179: Campus Hierarchy (APIs) | AID-325: Campus Admin Panel (UI) |
| AID-183: Academic Calendar (APIs) | AID-338: Calendar Admin (UI) |

Both are created together — never backend-only.

---

## 3. The Complete Workflow (End-to-End)

### Phase 1: BRD → Epic → Stories

```
1. BRD (Business Requirements Document) is provided
2. Kiro reads the BRD and creates:
   - 1 Epic in Jira
   - N User Stories under the Epic (each ≤ 8 story points)
   - For each Story: 4 default subtasks (Req Gen, Design, Code Review, Testing)
   - Backend + Frontend story pairs for every module
3. Lead/Manager assigns stories to team members
```

### Phase 2: Requirement Generation

```
4. Team member is assigned a story
5. Kiro creates feature branch: feature/{STORY-KEY}-{short-name}
6. Kiro generates the Requirement Document locally:
   - File: docs/requirements/{STORY-KEY}-{title}-requirements.md
   - Covers: functional requirements, acceptance criteria, validation rules, NFRs
7. Member reviews locally, approves
8. Kiro uploads .md to Jira subtask attachment
9. Kiro transitions subtask to "Pending Approval" and assigns to lead
10. Lead reviews → Approves or Rejects
    - Approved → Design starts
    - Rejected → Revise and resubmit
```

### Phase 3: Design Derivation

```
11. Requirement approved → Design subtask unlocked
12. Kiro generates the Design Document:
    - File: docs/design/{STORY-KEY}-{title}-design.md
    - Covers: architecture, API endpoints, data model (DDL), services, cross-cutting, testing strategy
13. Member reviews locally, approves
14. Kiro uploads to Jira, transitions to "Pending Approval", assigns to lead
15. Lead approves → Development starts
```

### Phase 4: Development

```
16. Design approved → Kiro creates development subtasks in Jira
    (Migrations, Entities, Services, Controllers, etc.)
17. Kiro assigns all subtasks to the team member
18. Kiro implements the code:
    - Backend: entities, repos, services, controllers, DTOs, mappers
    - Frontend: pages, hooks, schemas, components
19. Each dev subtask is transitioned to "Done" as completed
```

### Phase 5: Unit Tests & Coverage

```
20. After all dev subtasks are done:
21. Kiro writes unit tests
22. Kiro generates Unit Test Results doc:
    - File: docs/testing/{STORY-KEY}-unit-test-results.md
    - Uploads to Jira subtask
23. Kiro generates Code Coverage doc:
    - File: docs/code-coverage/{STORY-KEY}-coverage.md
    - Uploads to Jira subtask
24. Both subtasks transitioned to "Done"
```

### Phase 6: AI Code Review

```
25. All dev + tests + coverage done
26. Kiro runs the semantic_reviewer (AI code review):
    - Analyzes all code changes
    - Produces a behavioral review with verdict
27. If NEEDS_CHANGES:
    - Lists issues found
    - Asks user permission to auto-fix
    - Fixes issues, re-runs until clean
28. If APPROVED/COMMENT:
    - Saves review doc: docs/code-review/{STORY-KEY}-ai-review.md
    - Attaches to Jira Code Review subtask
    - Assigns to human reviewer (lead/senior)
    - Transitions to "Pending Approval"
```

### Phase 7: Human Code Review

```
29. Human reviewer checks the code
30. Approves → Testing starts
31. Requests changes → Fix → Re-review
```

### Phase 8: Testing

```
32. End-to-end testing performed
33. Issues found → documented in docs/testing/{STORY-KEY}-testing-results.md
34. Fixed → Code Review again → Testing again
35. All clear → Story is Done
```

### Phase 9: Merge to Main

```
36. Story fully complete (all subtasks Done)
37. Build verified: backend produces package, frontend builds clean, all tests pass
38. Kiro asks: "Ready to merge to main?"
39. User confirms → Kiro merges feature branch to main
40. Feature branch deleted
41. Story status → Done
```

---

## 4. How Kiro Works (For Beginners)

### What is Kiro?
Kiro is an AI-powered development environment. It reads your steering files (rules), connects to Jira (project management), and helps you write code, manage tasks, and follow processes.

### What are Steering Files?
Markdown files in `.kiro/steering/` that tell Kiro HOW to work:
- What tech stack to use
- What naming conventions to follow
- What workflow steps are mandatory
- What Jira hierarchy to create
- What code review rules to enforce

### What are Hooks?
Automated triggers that fire on events:
- File saved → track for review
- Task completed → update Jira
- Code review step reached → run AI reviewer

### How Does Kiro Talk to Jira?
Through MCP (Model Context Protocol) — a server connection that lets Kiro create issues, update statuses, assign people, upload attachments, and query data in Jira programmatically.

### How Does Kiro Talk to GitHub?
Through the GitHub MCP — can create repos, push files, create branches, make PRs.

---

## 5. Quick Reference Commands

| Action | How |
|--------|-----|
| Run backend | IntelliJ: Run `UtmsApplication.main()` |
| Run frontend | Terminal in `code/frontend`: `node node_modules/vite/bin/vite.js --port 3000` |
| Run backend tests | IntelliJ: Right-click `src/test` → Run All Tests |
| Run frontend tests | Terminal: `pnpm test` or `node node_modules/vitest/vitest.mjs run` |
| Access Swagger UI | http://localhost:8080/swagger-ui.html |
| Access Frontend | http://localhost:3000/admin/master-data/campuses |
| Check Flyway history | `SELECT * FROM utms.flyway_schema_history ORDER BY installed_rank DESC;` |

---

## 6. Key Decisions & Why

| Decision | Reason |
|----------|--------|
| No TypeScript on frontend | Team preference — plain JS with Zod for runtime validation |
| Java 21 (not 25) | LTS version, stable for production |
| Feature branches per story | Protects main from broken code |
| AI code review before human | Catches obvious issues (N+1 queries, missing routes, accessibility) so human reviewer focuses on design decisions |
| Documents attached to Jira | Reviewers can see everything without leaving Jira |
| Soft-delete everywhere | Audit trail requires historical data preservation |
| BaseEntity on all tables | Consistent audit columns, DRY |
| 4 default subtasks per story | Ensures every story follows the same quality gates |

---

*Last updated: 16 August 2026*
