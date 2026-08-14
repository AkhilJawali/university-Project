---
inclusion: always
---

# Development Workflow — Epic → User Story → Done

This steering file defines the mandatory workflow for all development work in the UTMS project.
Every team member and Kiro must follow this flow. No shortcuts.

---

## Structure

Every User Story in Jira has exactly **4 default subtasks** created at story creation time. Development subtasks are added later from the approved design document, at the same level as the default subtasks.

```text
Epic
└── User Story (≤ 8 Story Points, Testable)
    ├── Subtask: Requirement Generation
    ├── Subtask: Requirement Design Derivation
    ├── Subtask: Code Review
    ├── Subtask: Testing
    │
    │   (Added after design approval — same level:)
    ├── Subtask: Development Task 1
    ├── Subtask: Development Task 2
    ├── Subtask: Development Task 3
    ├── Subtask: Unit Test
    └── Subtask: Code Coverage
```

The 4 default subtasks are created when the story is created. Development subtasks are created after the design is approved — as siblings at the same level as the Requirement/Design/Review/Testing subtasks. Work begins only when the story is assigned to a team member.

---

## Jira Issue Type Mapping

| Level | Jira Issue Type | Parent |
|-------|----------------|--------|
| Top | Epic | None |
| Mid | Story | Epic (via parent ID) |
| Bottom | Subtask | Story (via parent ID) |

**Important:** When creating subtasks via API, always use `"parent": {"id": "<numeric_id>"}` — never use `"key"`. This is a known requirement for the AID project.

---

## Flow

### 1. Epic & User Story

- Epics are created from the BRD.
- Epics are broken into **testable User Stories**, each ≤ 8 story points.
- Each User Story gets the 4 default subtasks immediately on creation.
- Work starts only when the story is assigned.

### 2. Requirement Generation (Default Subtask 1)

- The assigned team member creates the Requirement Document locally.
- File location: `docs/requirements/{ISSUE-KEY}-{kebab-case-title}-requirements.md`
- Uploads/syncs it to the Requirement Generation subtask in Jira.
- Assigns the subtask to the Team Lead for review.
- **Lead approves → Design starts.**
- **Lead rejects → Update document → Resubmit.**

### 3. Requirement Design Derivation (Default Subtask 2)

- The assigned team member creates the Design Document based on the approved requirements.
- File location: `docs/design/{ISSUE-KEY}-{kebab-case-title}-design.md`
- Uploads/syncs it to the Design Derivation subtask in Jira.
- Assigns the subtask to the Team Lead for review.
- **Lead approves → Development starts.**
- **Lead rejects → Update document → Resubmit.**

### 4. Code Development (Subtasks created from Design)

- Once the design is approved, Kiro generates development subtasks from the approved Design Document.
- Development subtasks are created **at the same level** as the default subtasks (all are Subtask type under the Story).
- Kiro completes the development subtasks one by one.
- Once all development subtasks are completed:
  - **Unit Testing** is performed (created as a subtask).
  - Any issues found are documented, fixed, and retested.
  - A **Code Coverage** subtask is created and a coverage document is generated.

#### Code Coverage Document

- Location: `docs/code-coverage/{ISSUE-KEY}-{task-id}-coverage.md`
- Must include: task ID, story key, coverage percentage, covered classes/methods, uncovered areas.

### 5. Code Review (Default Subtask 3)

- After development and unit testing, the code goes for Code Review.
- **No issues found → Continue to Testing.**
- **Issues found:**
  - Issues are documented in: `docs/code-review/{ISSUE-KEY}-code-review.md`
  - Issues are fixed and retested.
  - Code goes for review again until clean.

#### Code Review Document

- Location: `docs/code-review/{ISSUE-KEY}-code-review.md`
- Must include: reviewer, date, issues found (with file/line references), severity, fix status.

### 6. Testing (Default Subtask 4)

- Complete code is tested end-to-end.
- **No issues found → Story is Done.**
- **Issues found:**
  - Issues are documented in: `docs/testing/{ISSUE-KEY}-testing-results.md`
  - Issues are fixed → Code Review again → Testing again.
  - Continue until all issues are resolved.

#### Testing Results Document

- Location: `docs/testing/{ISSUE-KEY}-testing-results.md`
- Must include: test scenarios, pass/fail, issues found, fix references.

---

## Final Flow (Linear)

```
BRD → Epic → User Story
  → Requirement Generation → Requirement Approval
  → Design Derivation → Design Approval
  → Development Subtasks (from design)
  → Unit Test → Code Coverage
  → Code Review
  → Testing
  → Done
```

---

## Jira Rules

### Default Subtasks (Created for Every User Story)

Every User Story must have these 4 subtasks created at story creation time, regardless of assignment:

| # | Subtask Name | Purpose |
|---|-------------|---------|
| 1 | Requirement Generation — {Story Title} | Requirement document creation and approval |
| 2 | Requirement Design Derivation — {Story Title} | Design document creation and approval |
| 3 | Code Review — {Story Title} | Peer review and issue resolution |
| 4 | Testing — {Story Title} | End-to-end testing and issue resolution |

### Development Subtasks (Created After Design Approval)

After the design is approved, Kiro creates additional subtasks under the same Story at the same level:
- One subtask per development task derived from the design document
- A "Unit Test" subtask
- A "Code Coverage" subtask
- All are Subtask issue type with parent = Story ID

### Assignment Rule

- All subtasks inherit assignment from the parent User Story (per squad rules).
- Work on any subtask begins only after the story is assigned.
- The flow is sequential — each phase is blocked until the previous one is completed/approved.

### Gating Rules

| Gate | Condition to Pass |
|------|-------------------|
| Requirement → Design | Requirement Generation subtask status = "Approved" |
| Design → Development | Design Derivation subtask status = "Approved" |
| Development → Unit Test | All development subtasks complete |
| Unit Test → Code Coverage | Unit tests pass, unit test doc generated |
| Code Coverage → Code Review | Coverage doc generated, meets 80% target |
| Code Review → Testing | Code review clean (no open issues) |
| Testing → Done | All test scenarios pass, no open issues |

---

## Folder Structure for Artifacts

```text
docs/
├── requirements/          # Requirement documents
│   └── {ISSUE-KEY}-{title}-requirements.md
├── design/                # Design documents
│   └── {ISSUE-KEY}-{title}-design.md
├── code-coverage/         # Coverage reports per task
│   └── {ISSUE-KEY}-{task-id}-coverage.md
├── code-review/           # Code review findings
│   └── {ISSUE-KEY}-code-review.md
└── testing/               # Testing results
    └── {ISSUE-KEY}-testing-results.md
```

---

## Kiro Enforcement

**Project Java Version Rule:** The project must target **Java 17 or Java 21 (LTS only)**. Never set the Java version to a non-LTS release (e.g., 22, 23, 24, 25). If the developer's local JDK is newer, the pom.xml `<java.version>` property ensures compilation targets the correct LTS version. Always use Spring Boot 3.x (latest stable GA for Java 21).

Kiro must:

1. **Never start design** unless the Requirement Generation subtask is "Approved".
2. **Never start code development** unless the Design Derivation subtask is "Approved".
3. **Never proceed to code review** until all development subtasks are done and coverage doc exists.
4. **Never mark testing as done** if there are unresolved issues.
5. **Create development subtasks in Jira** under the Story (same level as default subtasks) based on the approved design. Always use `"parent": {"id": "<story_numeric_id>"}`.
6. **Generate coverage documents** after unit testing completes.
7. **Document code review issues** in `docs/code-review/` if any are found.
8. **Document testing issues** in `docs/testing/` if any are found.
9. **Always attach documents to Jira subtasks.** When a document (requirement, design, unit test results, code coverage, code review) is generated:
    - Save as `.md` in the repo (for version control).
    - Do NOT create `.txt` copies — only `.md` files.
    - Once the document is reviewed and approved locally by the member, upload the `.md` file to the corresponding Jira subtask in the Attachments section.
    - This is mandatory — no subtask should be submitted for lead review without the `.md` attached in Jira.
9. **Transition subtasks to Done** in Jira immediately after completing the code changes for each subtask. Never leave a completed subtask in "To Do" or "In Progress".
10. **Generate a unit test results document** after completing unit tests. File: `docs/testing/{ISSUE-KEY}-unit-test-results.md`. Upload it to the Unit Test subtask description in Jira, add a comment referencing the file, attach the file to the subtask, then transition to Done.
11. **Generate a code coverage document** after unit testing completes. File: `docs/code-coverage/{ISSUE-KEY}-{task-id}-coverage.md`. Must include: covered classes/methods, coverage percentage (estimated or actual), uncovered gaps with plan, and requirement traceability. Upload it to the Code Coverage subtask description in Jira, add a comment, attach the file, then transition to Done.
12. **Always update Jira status** based on subtask type:
    - **Requirement Generation / Design Derivation subtasks:**
      - Created → **To Do** (default)
      - Document completed, assigned to lead → **Pending Approval**
      - Lead approves → **Approved** (done by lead)
      - Lead rejects → **Rejected** (done by lead)
    - **Development subtasks (from design doc):**
      - Created → **To Do** (default)
      - Code changes completed → **Done**
    - **Code Review subtask:**
      - Created → **To Do** (default)
      - Submitted to reviewer → **Pending Approval**
      - Reviewer approves → **Approved** (done by reviewer)
    - **Testing subtask:**
      - Created → **To Do** (default)
      - All tests pass, no issues → **Done**
    - Never leave a ticket in a stale status. If work is done, the ticket must reflect it immediately.
13. **Parent Story status must reflect progress.** If any subtask under a Story moves out of "To Do" (e.g., to In Progress, Pending Approval, Approved, Done), the parent Story must be transitioned to "In Progress" if it's still in "To Do". A Story should never remain in "To Do" once work has begun on any of its subtasks.
