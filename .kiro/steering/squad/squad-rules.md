---
inclusion: always
---

# Squad Rules

## Working Agreements
- All work is driven from Jira tickets — no untracked work enters the sprint
- Every task maps to a BRD requirement or a technical enabler; traceability is mandatory
- Prefer small, focused PRs (< 400 lines) over large monolithic changes
- Blocked items are raised in standup immediately, not end-of-day
- All communication about decisions lives in Jira comments or ADRs, not Slack/email threads
- Demo what you ship — every completed story gets a brief walkthrough in sprint review

## Code Review Standards
- All PRs require at least 1 approval (2 for security-sensitive or schema changes)
- Reviewers respond within 4 business hours
- Author resolves all comments before merge
- Use "Request Changes" for blocking issues, "Comment" for suggestions
- PRs must include:
  - Link to Jira ticket
  - Brief description of what changed and why
  - Steps to test (or automated test evidence)
- No self-merging unless explicitly approved by tech lead for hotfixes

## Definition of Done
- [ ] Code complete and passes linting
- [ ] Unit tests written and passing (minimum 80% coverage on new code)
- [ ] Integration tests passing where applicable (API endpoints, DB queries)
- [ ] No new critical/high Sonar issues introduced
- [ ] API documentation updated (OpenAPI spec for new/modified endpoints)
- [ ] Database migrations reviewed and reversible
- [ ] PR reviewed and approved
- [ ] Jira ticket updated and moved to Done
- [ ] No regression in existing functionality (CI pipeline green)
- [ ] Security checklist verified (input validation, parameterized queries, no secrets in code)

## Sprint Ceremonies (2-Week Sprints)

| Ceremony | Cadence | Duration | Attendees |
|----------|---------|----------|-----------|
| Standup | Daily (Mon–Fri) | 15 min | Full squad |
| Sprint Planning | Day 1 of sprint | 2 hrs | Full squad + PO |
| Backlog Refinement | Mid-sprint (Wed, Week 1) | 1 hr | Full squad + PO |
| Sprint Review / Demo | Last day of sprint | 1 hr | Squad + stakeholders |
| Retrospective | Last day of sprint (after review) | 45 min | Full squad |

## Branch Strategy
- `main` — production-ready, protected
- `develop` — integration branch, all feature PRs target here
- `feature/<TICKET-ID>-short-description` — feature branches
- `bugfix/<TICKET-ID>-short-description` — bug fixes
- `hotfix/<TICKET-ID>-short-description` — production hotfixes (cherry-picked to main)

## Commit Message Format
```
<type>(<TICKET-ID>): <short description>

[optional body]

[optional footer: breaking changes, references]
```
Types: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `perf`, `ci`

## Subtask Assignment Inheritance

**If a User Story is assigned to a team member, all subtasks under that story are implicitly assigned to the same person.**

This means:
- Kiro does not need to check subtask-level assignment separately — checking the parent story assignment is sufficient.
- If the parent story is assigned to you, you are authorized to work on any subtask (Requirement Generation, Design Derivation, etc.) under that story.
- This applies to access control checks: verifying story assignment grants access to all its subtasks.

### Auto-Assignment on Story Assignment

**When a story is assigned to someone, all its subtasks must also be assigned to that person by default.**

- When Kiro assigns a story (or is told a story has been assigned), it must also assign all subtasks under that story to the same assignee **in Jira** — not just implicitly.
- This includes all 4 default subtasks (Requirement Generation, Design Derivation, Code Review, Testing) and any development subtasks already created.
- The assignee can later be changed on individual subtasks if needed (e.g., Code Review assigned to a reviewer).
- This is a default behavior — not a lock. Reassignment is always allowed.
- **Kiro enforcement:** After assigning a story, query its children (`parent = {STORY-KEY}`) and assign each one to the same person.

---

## Requirement Document Access Control

### The Rule

**If you are not assigned to a User Story in Jira, you are blocked from creating or editing its requirement document. No exceptions.**

### How It's Enforced

Kiro checks Jira assignment (via MCP) before any requirement document work. The current user is identified by their `JIRA_EMAIL` in their MCP config.

When any team member asks to create or edit a requirement doc:

1. Kiro identifies the Jira story key (from the request or filename).
2. Kiro queries Jira to check who the story is assigned to.
3. Compares the assignee against the current user's `JIRA_EMAIL`.
4. **If assigned to them** → proceeds.
5. **If not assigned** → refuses entirely. No drafts, no workarounds, no temp files.

### Workflow

```
1. Story Assigned     → PO/Lead assigns a User Story to a team member in Jira
2. Requirement Doc    → The assigned member (and ONLY them) creates the requirement doc
3. Submit for Review  → Author assigns the Jira subtask to their lead
4. Review             → Lead reviews the requirement doc
5. Approval/Rejection → Lead approves or requests changes via Jira transition
6. Iterate            → If rejected, author revises and resubmits
```

### Rules

1. **Not assigned = blocked.** If the story is unassigned or assigned to someone else, you cannot create, edit, or append to its requirement file.
2. **One owner per story.** Only one team member is assigned the story at a time.
3. **Submit to lead, not self-approve.** After writing the doc, assign the subtask to the lead for review.
4. **Wait for approval before proceeding.** Do not begin design derivation or code until the requirement doc is approved (status = "Approved").
5. **This is a team-wide rule.** Everyone gets it. No one is exempt.

---

## Escalation Path
1. Blocker raised in standup
2. If unresolved in 4 hours → escalate to tech lead
3. If cross-squad dependency → escalate to engineering manager
4. If timeline risk → PO and stakeholders informed same day
