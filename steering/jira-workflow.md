---
inclusion: auto
name: jira-workflow
description: Update Jira issues — change status/transition, edit fields, add comments, reassign. Use whenever the user says things like "update my Jira", "update the task", "move the ticket", "change the status", "comment on", or mentions a Jira issue key.
---

# Jira Workflow (updates & transitions)

Guides Kiro for day-to-day Jira operations on issues that already exist. All
actions use the Atlassian MCP server.

Scope note: this file is for **updating** existing issues. Creating a full
Epic/Story/Task hierarchy from a BRD is handled by the separate `brd-to-jira`
steering file — do not duplicate that flow here.

## Golden rules

- Every write (transition, field edit, comment, assignment) is shown to the
  user for confirmation before it runs. State the issue key, the current
  value, and the new value.
- Identify the target issue first. If the user gives an issue key (e.g.
  `DMS-42`), use it. If they describe it in words, search via the MCP and
  confirm the match before changing anything.
- Never invent or substitute a status that isn't in the issue's workflow. If a
  requested status isn't valid for that issue type, say so and list the valid
  options.
- Only change assignee, priority, or fields the user actually asked about.
  Do not touch anything else.

## Board structure (context)

Issues in this project follow this hierarchy:

```
Epic
 └── User Story  (<= 5 story points, testable)
      ├── Task: Requirement Generation
      ├── Task: Requirement Design Derivation
      └── Task: Code Development
           ├── Sub-task: Unit Test
           └── Sub-task: API Documentation
```

## Valid statuses per issue type

Use these when transitioning. Do not apply a status to an issue type it
doesn't belong to.

- **Requirement Generation** and **Requirement Design Derivation** tasks:
  `In Progress`, `PENDING APPROVAL`, `Approved`, `Hold`, `Rejected`.
- **Unit Test** and **API Documentation** sub-tasks:
  `In Progress`, `DONE`.

If the user asks to move an issue to a status not in its list, do not force
it — tell them the valid statuses for that issue type and ask how to proceed.

## Transitioning status

1. Fetch the issue and report its current status.
2. Confirm the requested target status is valid for that issue type (see above).
3. Show the user: `<KEY>: <current status> -> <target status>` and wait for a yes.
4. Perform the transition via the MCP, then confirm with the new status + link.

Batch requests ("move all my code-dev sub-tasks to In Progress"): list every
affected issue and its intended transition, get a single approval, then apply
them one at a time and report each result.

## Editing fields

- When updating descriptions, summaries, or estimates, show the before/after.
- Keep story-point estimates <= 5 for user stories; if an edit would exceed 5,
  flag it and suggest splitting the story instead.

## Comments

- When asked to comment, write clear, complete sentences stating what changed
  and why.
- When a status change relates to code work, add a short comment noting the
  branch or PR if the user provides it.

## Never do automatically

- Never move an issue to a terminal/`Done`-type status on your own — leave
  final sign-off to the user.
- Never reassign, change priority, or close issues unless explicitly asked.
- Never delete issues.
