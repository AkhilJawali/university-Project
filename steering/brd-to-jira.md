---
inclusion: auto
name: brd-to-jira
description: Generate a Jira breakdown from a Business Requirements Document (BRD). Use whenever the user provides or references a BRD and asks to create an epic, user stories, tasks, or sub-tasks in Jira.
---

# BRD → Jira Breakdown

This steering guides Kiro to turn a Business Requirements Document (BRD) into a
structured Jira hierarchy using the Atlassian MCP server.

## Golden rules

- All Jira writes go through the Atlassian MCP server.
- This is a **two-phase** workflow. First produce the full breakdown as a
  reviewable outline and get the user's approval. Only then create anything
  in Jira.
- Never create Jira issues silently. Create them top-down (Epic → Stories →
  Tasks → Sub-tasks) and report the issue key + link after each one.
- Ask for the target Jira **project key** before creating anything. If unknown,
  list available projects first.

## Input — find the BRD in the workspace

Search the workspace (project root, and common docs locations like `docs/`) for
a BRD document. Candidate files are `.md` or `.txt` files whose name suggests a
BRD (e.g. contains "brd", "business", or "requirements").

**Important — supported formats:**
- ✅ `.md` (Markdown) — readable directly
- ✅ `.txt` (Plain text) — readable directly
- ❌ `.docx` (Word) — binary format, cannot be read by Kiro
- ❌ `.pdf` — binary format, cannot be read by Kiro

**If a `.docx` or `.pdf` is found:**
Inform the user:
> "I found **{filename}** but it's a `.docx`/`.pdf` file which I can't read directly.
> Please save it as a `.md` or `.txt` file and place it in the workspace folder:
> `{workspace_path}`
> Then ask me again and I'll process it immediately."

Do NOT ask the user to paste the content — direct them to convert the file instead.

1. **Exactly one readable BRD found** → use it, and state which file:
   > "Using **{filename}** as the BRD. I'll analyze it now."
2. **Multiple candidates found** → list them and ask which to use.
3. **None found, but a .docx/.pdf exists** → show the conversion message above.
4. **Nothing found at all** → ask the user to add the BRD to the workspace folder.

Once the file is chosen, read it fully and identify the goal, scope, actors,
functional requirements, and any non-functional requirements. Do not invent
requirements — everything must come from the document.

## Phase 1 — Analyze and draft (no writes)

Produce a nested outline for review:

### 1. Epic

- Derive **one Epic** that captures the overall initiative described in the BRD.
- Give it a clear name and a one-paragraph goal summarizing the BRD.

### 2. User Stories

- Generate **all** user stories that the BRD implies — cover the full scope.
- Write each in the form: *"As a `<role>`, I want `<capability>` so that
  `<benefit>`."*
- **Sizing:** each story must be **≤ 5 story points**. If a story would exceed
  5 points, split it into smaller stories until each is ≤ 5.
- **Testability:** each story must be independently testable. Include clear
  **acceptance criteria** (Given/When/Then) that a tester can verify. If a
  story cannot be expressed with testable acceptance criteria, split or
  rewrite it until it can.

### 3. Tasks under each User Story

For every user story, create the following tasks, each with a written
description tying it back to the story and BRD:

- **Requirement Generation** — capture and document the detailed requirements
  for this story.
- **Requirement Design Derivation** — derive the technical/functional design
  from the requirements.
- **Code Development** — implement the story.

### 4. Sub-tasks under Code Development

Under each **Code Development** task, create these sub-tasks:

- **Unit Test** — write and run unit tests for the developed code.
- **API Documentation** — document the APIs produced by the development.

Present all of the above as an indented tree so the user can review and edit
before anything is created.

## Phase 2 — Create in Jira (after approval)

Create issues in this order and link them correctly:

1. Create the **Epic**.
2. Create each **User Story** linked to the Epic (parent/epic-link field).
   Set the story-point estimate (≤ 5).
3. Under each Story, create the three **Tasks** with their descriptions.
4. Under each **Code Development** task, create the two **Sub-tasks**.

Report each created issue's key and URL as you go. Pause and ask if any
required field (project, issue type, estimate field) is missing or ambiguous.

## Statuses (workflow)

These statuses already exist in the project's workflow, so no status creation
is needed:

- **Requirement Generation** and **Requirement Design Derivation** tasks:
  `In Progress`, `PENDING APPROVAL`, `Approved`, `Hold`, `Rejected`.
- **Unit Test** and **API Documentation** sub-tasks:
  `In Progress`, `DONE`.

Do not transition issues into any status automatically unless the user asks —
creation should leave issues in their default initial status.

## Output summary

After creation, give the user a summary table: Epic key, and for each Story its
key, point estimate, its Task keys, and the Sub-task keys — each with a link.
