---
inclusion: manual
name: api-doc-completion
---

# API Documentation Completion Guide

Run this to generate the API documentation, attach it to the **API
Documentation** sub-task (under the Code Development task in the board
hierarchy), and mark that sub-task `Done`. All Jira actions use the
Atlassian MCP server.

There is no gate on this step — just generate the doc, upload it to the ticket,
and complete it. Sub-task statuses are `In Progress` and `Done`.

---

## Step 1 — Generate the API documentation

Produce the API documentation from the implemented endpoints. The backend uses
springdoc-openapi, so use the generated OpenAPI spec as the source of truth.

- Prefer exporting the generated **OpenAPI spec** (`openapi.json` / `openapi.yaml`
  from `/v3/api-docs`) as the primary artifact.
- Also write a readable reference file `api-docs-{ISSUE-KEY}.md` to the repo root,
  covering each endpoint:
  - Method + path (e.g. `POST /api/v1/payments`)
  - Request DTO and response DTO
  - Status codes and error responses
  - Authentication / authorization required
  - A short description of what the endpoint does

Keep the documentation in sync with the actual implemented controllers.

---

## Step 2 — Locate the API Documentation sub-task

Find the sub-task automatically:

1. Identify the Code Development task for the story in scope.
2. Search its sub-tasks via the MCP:
   `parent = {CODE-DEV-KEY} AND summary ~ "API Documentation"`
3. **Exactly one match** → use it, and state which one.
4. **No match** → ask the user for the sub-task issue key.
5. **Multiple matches** → list them and ask the user to pick.

The resolved key becomes `{ISSUE-KEY}`.

---

## Step 3 — Confirm, then sync to Jira

### 3a. Pre-sync confirmation (required)

Show the user what's about to happen, then wait for a "yes":

> "About to finalize the API Documentation sub-task **{ISSUE-KEY}**:
> - API docs → attached ({file(s)})
> - Comment → added
> - Status → {current status} → Done
>
> Proceed? (yes / no)"

### 3b. Apply, then report the actual result

1. **Attach the documentation** to the sub-task.
   - If the MCP supports file attachments, attach the OpenAPI spec and/or the
     `api-docs-{ISSUE-KEY}.md` file.
   - If attachments are **not** supported by the MCP, do not fail silently —
     add the doc summary in the comment and include the file path / link so it's
     traceable.
2. **Add a comment** noting the documentation:
   > "API documentation generated and attached. Source: api-docs-{ISSUE-KEY}.md
   > (OpenAPI spec included). Endpoints documented: {count}."
3. **Transition the sub-task to `Done`:**
   - Fetch available transitions first.
   - If `Done` is available, apply it.
   - If not, do NOT substitute another status — stop and tell the user it's a
     workflow-wiring issue and ask how to proceed.
4. Confirm with the **actual** resulting status and the issue link.

---

## Notes

- Never write to Jira before the pre-sync confirmation (Step 3a).
- If the MCP can't attach files, put the doc summary in the comment plus a
  link/path — never claim an attachment succeeded when it didn't.
- Never invent or silently substitute a Jira status; only use valid transitions.
- This handles the API Documentation sub-task only. The Unit Test sub-task is
  finalized separately (see `unit-test-completion`).
