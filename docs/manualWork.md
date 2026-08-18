# Manual Work — Human Interaction Points

This document lists every step in the UTMS development workflow where human interaction is required. Kiro cannot proceed past these points without explicit input from a team member or lead.

---

## 1. Story Assignment

| Action | Who | Why |
|--------|-----|-----|
| Assign a User Story to a team member in Jira | PO / Tech Lead | Work cannot begin on any subtask until the story is assigned. This is the gate that unlocks the entire flow. |

---

## 2. Requirement Generation Phase

| # | Interaction Point | Who | What Happens |
|---|-------------------|-----|--------------|
| 2.1 | **Select the User Story** | Developer | Kiro presents assigned stories; the developer picks which one to write requirements for. |
| 2.2 | **Provide custom prompt (optional)** | Developer | Developer may provide special instructions for the requirements format. If skipped, default format is used. |
| 2.3 | **Approve the requirements document** | Developer | After Kiro generates the doc, the developer must say "yes" / "no" / "request changes" in chat before anything goes to Jira. |
| 2.4 | **Provide assignee email for Jira sync** | Developer | Kiro asks who the Requirement Generation subtask should be assigned to (typically the lead for review). |
| 2.5 | **Confirm the Jira sync (pre-sync confirmation)** | Developer | Kiro shows exactly what will be written to Jira (description, comment, assignee, status transition) and waits for a final "yes". |
| 2.6 | **Lead reviews and approves/rejects requirements** | Tech Lead | Lead reviews the attached document and transitions the subtask to "Approved" or "Rejected". This is a manual Jira action by the lead. |
| 2.7 | **Revise and resubmit (if rejected)** | Developer | If the lead rejects, the developer must update the document and repeat steps 2.3-2.6. |

---

## 3. Design Derivation Phase

| # | Interaction Point | Who | What Happens |
|---|-------------------|-----|--------------|
| 3.1 | **Identify the story in scope** | Developer | Developer confirms or selects which story to design for. |
| 3.2 | **Approve the design document** | Developer | After Kiro generates the design doc, the developer must approve it in chat before Jira sync. |
| 3.3 | **Provide assignee email for Jira sync** | Developer | Developer provides the email for who should review the design (typically lead). |
| 3.4 | **Confirm the Jira sync (pre-sync confirmation)** | Developer | Kiro shows pending Jira changes; developer must say "yes" to proceed. |
| 3.5 | **Lead reviews and approves/rejects design** | Tech Lead | Lead reviews the design document and transitions the subtask to "Approved" or "Rejected" in Jira. |
| 3.6 | **Revise and resubmit (if rejected)** | Developer | If rejected, developer updates the design and repeats steps 3.2-3.5. |

---

## 4. Code Development Phase

| # | Interaction Point | Who | What Happens |
|---|-------------------|-----|--------------|
| 4.1 | **Confirm development subtask creation** | Developer | After design approval, Kiro proposes development subtasks derived from the design. Developer confirms before they are created in Jira. |

---

## 5. Code Review Phase (AI + Human)

| # | Interaction Point | Who | What Happens |
|---|-------------------|-----|--------------|
| 5.1 | **AI review finds issues — fix decision** | Developer | If the AI `semantic_reviewer` returns NEEDS_CHANGES, Kiro lists the issues and asks: "Do you want these auto-fixed?" Developer must respond yes/no. |
| 5.2 | **Human code review** | Reviewer (peer / lead) | After AI review passes, the Code Review subtask is assigned to a human reviewer. They review the PR and approve or request changes. This is entirely manual. |
| 5.3 | **Fix review findings and resubmit** | Developer | If the reviewer requests changes, the developer must fix issues and submit for review again. |

---i

## 6. Testing Phase

| # | Interaction Point | Who | What Happens |
|---|-------------------|-----|--------------|
| 6.1 | **End-to-end testing execution** | Tester / Developer | Complete functional testing of the code. Results are documented, but the actual testing and pass/fail judgment requires human validation. |
| 6.2 | **Verify test results and close** | Tech Lead / Tester | Someone must confirm all test scenarios pass and no open issues remain before the Testing subtask can be marked Done. |
| 6.3 | **Fix testing issues and re-route** | Developer | If testing finds issues, developer fixes them, then code goes through Code Review again, then Testing again. Each cycle requires human sign-off. |

---

## 7. Jira Workflow Transitions (Manual by Leads)

These transitions can only be performed by the Tech Lead or designated reviewer — Kiro cannot self-approve:

| Transition | Who | When |
|------------|-----|------|
| Requirement Generation: To Do → Pending Approval | Developer (via Kiro) | After document is synced |
| Requirement Generation: Pending Approval → **Approved** | Tech Lead | After reviewing the document |
| Requirement Generation: Pending Approval → **Rejected** | Tech Lead | If document needs rework |
| Design Derivation: Pending Approval → **Approved** | Tech Lead | After reviewing the design |
| Design Derivation: Pending Approval → **Rejected** | Tech Lead | If design needs rework |
| Code Review: Pending Approval → **Approved** | Reviewer | After human code review passes |
| Testing: To Do → **Done** | Tester / Lead | After all tests pass |
| Story: In Progress → **Done** | Tech Lead | After all subtasks complete |

---

## 8. Sprint Ceremonies (Recurring Human Activities)

| Ceremony | Frequency | Human Action Required |
|----------|-----------|----------------------|
| Daily Standup | Every weekday | Report progress, raise blockers |
| Sprint Planning | Day 1 of sprint | Commit to stories, estimate effort |
| Backlog Refinement | Mid-sprint | Clarify requirements, split stories |
| Sprint Review / Demo | Last day | Demo completed features to stakeholders |
| Retrospective | Last day | Reflect and identify improvements |

---

## 9. Escalation Decisions

| # | Situation | Who | Action |
|---|-----------|-----|--------|
| 9.1 | Blocker unresolved for 4 hours | Tech Lead | Escalate and unblock the team |
| 9.2 | Cross-squad dependency | Engineering Manager | Coordinate resolution between squads |
| 9.3 | Timeline risk identified | PO + Stakeholders | Informed same day, scope/timeline negotiation |

---

## 10. Local Database Setup (Manual)

Kiro generates Flyway migration files and application code, but the actual PostgreSQL database must be set up locally by the developer. This is a manual prerequisite before any backend code can run.

| # | Action | Who | Notes |
|---|--------|-----|-------|
| 10.1 | **Install PostgreSQL 15+** | Developer | Install locally or via Docker (`docker run -d --name utms-postgres -p 5432:5432 -e POSTGRES_PASSWORD=... postgres:15`) |
| 10.2 | **Create the `utms` database** | Developer | `CREATE DATABASE utms;` — the application expects this database to exist |
| 10.3 | **Create the `utms` schema** | Developer | `CREATE SCHEMA IF NOT EXISTS utms;` — all tables live under this schema |
| 10.4 | **Configure connection credentials** | Developer | Set `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password` in `application.yml` or environment variables. Never commit real credentials. |
| 10.5 | **Run Flyway migrations** | Developer | `mvn flyway:migrate` or let Spring Boot auto-run on startup. Verify all migrations succeed. |
| 10.6 | **Install and start Redis** | Developer | Required for caching and job queues. Install locally or via Docker (`docker run -d --name utms-redis -p 6379:6379 redis:7`) |
| 10.7 | **Verify connectivity** | Developer | Start the Spring Boot app and confirm it connects to both Postgres and Redis without errors |

> **Docker Compose alternative:** If a `docker-compose.yml` is available at the project root, run `docker compose up -d` to start both Postgres and Redis in one command. You still need to verify migrations run correctly.

---

## 11. Infrastructure and Environment (One-Time / Occasional)

| # | Action | Who | Notes |
|---|--------|-----|-------|
| 11.1 | Provision PostgreSQL, Redis, Docker environment | DevOps / Developer | Local dev setup and CI/CD pipeline configuration |
| 11.2 | Configure secrets in environment / secrets manager | DevOps / Lead | API keys, DB credentials, JWT secrets — never committed to code |
| 11.3 | Set up CI/CD pipeline (GitHub Actions) | DevOps / Lead | Build, test, lint, security scan stages |
| 11.4 | Configure Jira project workflows | Project Admin | Ensure status transitions match the expected flow (To Do → In Progress → Pending Approval → Approved/Rejected → Done) |
| 11.5 | MCP server configuration | Developer | Set up `.kiro/settings/mcp.json` with Jira credentials and approved servers |

---

## Summary

**Total human interaction categories: 10**

The workflow is designed so that Kiro handles all automatable work (document generation, code implementation, Jira updates, test execution), but humans retain control over:

1. **Decisions** — which story to work on, approval of artifacts
2. **Quality gates** — requirement review, design review, code review, testing sign-off
3. **Jira approvals** — only leads/reviewers can transition tasks past approval gates
4. **Ceremonies** — sprint rituals require team participation
5. **Infrastructure** — environment and secrets management
