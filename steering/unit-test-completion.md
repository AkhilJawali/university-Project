---
inclusion: manual
name: unit-test-completion
---

# Unit Test Completion Guide

Run this once the user is satisfied with the code. It uploads the code coverage
report and test details to the **Unit Test** sub-task (which sits under the
Code Development task in the board hierarchy) and marks that sub-task
`Done`. All Jira actions use the Atlassian MCP server.

Sub-task statuses are `In Progress` and `Done`.

---

## Step 1 — Confirm the code is ready

Confirm with the user that development is done and they're satisfied with the
code before proceeding:

> "Ready to finalize the Unit Test sub-task? I'll run the tests, check coverage,
> and if it passes I'll upload the report and mark the sub-task Done. (yes / no)"

Only continue on a yes.

---

## Step 2 — Run tests and generate the coverage report

Run the test + coverage task and capture the results:

- **Maven:** `mvn clean verify` (JaCoCo report at `target/site/jacoco/` —
  `index.html`, `jacoco.xml`, `jacoco.csv`).
- **Gradle:** `./gradlew test jacocoTestReport jacocoTestCoverageVerification`
  (report at `build/reports/jacoco/test/`).

Collect these details from the run:
- Total tests run, passed, failed, skipped.
- Line coverage % and branch coverage % (from the JaCoCo report).
- The report file location.

If tests fail, stop and report the failures — do not mark anything complete.

---

## Step 3 — Enforce the coverage gate (> 90%)

Per the `backend-tech` standard, unit test coverage must be **above 90%**.

- If coverage is **> 90%**, continue.
- If coverage is **90% or below**, stop and tell the user the current coverage
  and that the sub-task cannot be completed until it exceeds 90%. Do not
  transition the sub-task.

---

## Step 4 — Locate the Unit Test sub-task

Find the Unit Test sub-task automatically:

1. Identify the Code Development task for the story in scope.
2. Search its sub-tasks via the MCP:
   `parent = {CODE-DEV-KEY} AND summary ~ "Unit Test"`
3. **Exactly one match** → use it, and state which one.
4. **No match** → ask the user for the sub-task issue key.
5. **Multiple matches** → list them and ask the user to pick.

The resolved key becomes `{ISSUE-KEY}`.

---

## Step 5 — Confirm, then sync to Jira

### 5a. Pre-sync confirmation (required)

Show the user everything about to happen, then wait for a "yes":

> "About to finalize the Unit Test sub-task **{ISSUE-KEY}**:
> - Coverage report → attached ({report file})
> - Comment → test summary added
> - Status → {current status} → Done
>
> Coverage: {line}% line / {branch}% branch · Tests: {passed}/{total} passed
>
> Proceed? (yes / no)"

### 5b. Apply, then report the actual result

1. **Attach the coverage report** to the sub-task.
   - If the MCP supports file attachments, attach the JaCoCo report
     (`jacoco.xml` and/or the HTML `index.html`).
   - If attachments are **not** supported by the MCP, do not fail silently —
     add the full coverage summary in the comment (Step 5b.2) and include the
     report file path / CI artifact link so it's traceable.
2. **Add a comment** with the test details:
   > "Unit tests completed. Coverage: {line}% line / {branch}% branch (threshold 90%).
   > Tests: {total} run, {passed} passed, {failed} failed, {skipped} skipped.
   > Report: {report location / link}."
3. **Transition the sub-task to `Done`:**
   - Fetch available transitions first.
   - If `Done` is available, apply it.
   - If not, do NOT substitute another status — stop and tell the user it's a
     workflow-wiring issue and ask how to proceed.
4. Confirm with the **actual** resulting status and the issue link.

---

## Notes

- Only finalize when the user confirms the code is ready (Step 1) and coverage
  is above 90% (Step 3).
- Never write to Jira before the pre-sync confirmation (Step 5a).
- If the MCP can't attach files, put the coverage details in the comment plus a
  link/path — never claim an attachment succeeded when it didn't.
- Never invent or silently substitute a Jira status; only use valid transitions.
- This handles the Unit Test sub-task only. The API Documentation sub-task is
  finalized separately.
