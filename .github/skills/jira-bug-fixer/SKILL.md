---
name: jira-bug-fixer
description: "Resolve a Jira bug end to end: safely update main, read the ticket, implement a minimal fix, run unit and integration tests, push feature/<TICKET-ID>, open a templated PR, and request Copilot code review. Use when asked to fix or solve a Jira bug ticket, such as SELC-1234."
---

# Jira Bug Fixer

Follow the steps below in order. This skill authorizes the ticket-related fix,
the Jira transition to `In progress` at the start of step 2, commit, push,
PR creation, and review request, subject to the safety gates below.
Do not execute this workflow when the user only asks to describe or edit the skill.

## General rules

- Use this skill only when explicitly asked to fix a Jira bug ticket. Do not use it for
  general code review, refactoring, or other tasks.
- Follow repository instructions and existing conventions.
- Keep investigation proportional to the bug. Read enough to identify the cause
  and affected behavior; do not turn the task into an exhaustive audit or demand
  a perfectly specified ticket.
- Make the smallest complete fix. Avoid unrelated refactoring, formatting,
  dependency upgrades, or infrastructure changes.
- Never discard, stash, commit, or overwrite the user's work without approval.
  Do not force-push, amend existing commits, or commit directly to `main`.
- Stage only explicit ticket-related paths, never `git add .` or `git add -A`.
  Never commit secrets, local configuration, or generated test reports.
- Use the already configured Jira and GitHub MCP servers for all Jira and GitHub
  API operations, including ticket reads/transitions, PRs, and
  Copilot review requests. Discover the tools exposed by those servers as needed.
  Do not use `gh`, direct HTTP API calls, or new credentials as a fallback.
- Local repository operations (status, fetch, pull, branch, commit, and push)
  still use `git`. Do not invent ticket contents, repository names, tool names,
  commands, or results.
- If authentication, permissions, required tools, or external services block a
  step, explain the blocker and ask for the minimum input needed. Never report an
  unperformed step as successful.
- Do not merge the PR, deploy, or make other Jira status changes unless asked.
  The transition to `In progress` in step 2 is explicitly authorized.

## 0. Check pending work

Run from the repository root:

```bash
git status --short --untracked-files=all
git branch --show-current
git remote -v
```

If any staged, unstaged, or untracked work exists, show the affected paths and
**ask the user before proceeding**. Wait for their decision. Offer to stop while
they save their work, or preserve it using an explicitly approved approach, such
as a stash including untracked files or a separate worktree. Never assume approval
or include those changes in the bug fix.

If there is an unfinished merge, rebase, or cherry-pick, stop and ask the user to
resolve it. Confirm the GitHub repository and remote instead of assuming `origin`
points to the intended repository. Confirm access to that repository through a
read-only GitHub MCP operation before publishing.

## 0a. Update main

Only after the worktree is clean or the approved isolation is in place:

```bash
git fetch <remote>
git switch main
git pull --ff-only <remote> main
```

Replace placeholders with verified values. If local `main` does not exist, create
it tracking the verified remote's `main`. If `main` has local-only commits,
diverges, or cannot fast-forward, stop and ask; never reset or rebase it
automatically. Confirm local `HEAD` equals the freshly fetched remote `main`,
then record that SHA as the fix's base.

Read the repository instructions again if the update changed them.

## 1. Read the Jira ticket

Extract the ticket key from the request or Jira URL, normalizing it to the form
`SELC-1234`. Ask for the key only if missing or ambiguous.

Use the configured Jira MCP server to read the summary, description, reproduction
steps, and acceptance criteria. Read linked material or comments only when needed
to understand the bug. If the server is unavailable, report the blocker and ask
the user to restore access or provide the relevant ticket contents instead of
guessing. User-provided contents do not bypass the status transition in step 2.

Briefly state the observed problem, intended behavior, and likely affected
module. Reasonable non-behavioral assumptions are acceptable; ask only when an
ambiguity changes expected behavior, scope, or the implementation materially.

Set `BRANCH=feature/<TICKET-ID>` using the normalized key. Check whether this
branch already exists locally or remotely. If it does, ask whether to resume it
or stop; never overwrite it or silently choose a different name.
For an explicitly approved resume, switch to that branch and inspect its existing
changes before step 2. This is the only exception to creating a new branch after
testing in step 4.

## 2. Implement the minimal fix

Before investigating or editing code in this step, use the configured Jira MCP
server to transition the ticket to `In progress`. If it is already in that status,
leave it unchanged. Otherwise, fetch the available workflow transitions through
MCP and use the transition ID whose destination status is `In progress`
(case-insensitive); do not assume the transition name matches the status or guess
an ID.

Confirm the transition succeeded before implementing the fix. If it is
unavailable, ambiguous, or blocked by permissions, report the blocker and ask
the user how to proceed rather than silently skipping the status change.

Trace the relevant code and existing tests to find the root cause. Reproduce the
bug with an existing test or a focused regression test where practical.

Implement the smallest precise change that fixes the affected behavior, including
directly coupled callers or mappings when necessary. Add or update regression
coverage using the existing test framework. Update documentation only if the
change requires it.

Keep the changes uncommitted until step 4. If unrelated changes appear while
working, do not include them; stop and ask if they conflict with the fix.

## 3. Run unit and integration tests

Inspect the affected module's POM, test profiles, repository instructions, and
relevant `.github/workflows/` files to identify the actual test commands.

For Nx-managed modules, invoke `nx-workspace` to discover resolved targets with
`pnpm nx show project <project> --json`, and use `nx-run-tasks` to run the
appropriate existing targets via `pnpm nx run <project>:<target>`. Do not assume
that a target exists or that a generic `test` target includes integration tests.

If no appropriate Nx target exists, use the module's existing Maven commands.
For example, unit tests may use:

```bash
mvn -f apps/<module>/pom.xml test -Dtest=<RelevantTestClasses>
```

Integration tests vary by module. They may run through Failsafe's `verify` phase
with the module's skip flags disabled, or through Surefire with an explicit
`CucumberSuiteTest` selector. Match the module and its CI configuration rather
than assuming `mvn verify` runs Cucumber. For example, the product integration
workflow currently uses:

```bash
mvn --projects :product --also-make test -Dtest=CucumberSuiteTest -Dsurefire.failIfNoSpecifiedTests=false
```

Run both relevant unit and integration suites, including affected consumers when
changing shared code. Use the smallest meaningful scope; broaden it if failures
or cross-module effects require it. Ensure Docker and required test services are
available for Testcontainers/Cucumber.

Inspect the test summaries/reports to confirm the intended tests actually ran,
with no failures and no suite silently skipped. A successful process with zero
relevant tests is not sufficient. Record the exact commands and outcomes for the
PR body.

Fix failures caused by the change and rerun the affected suites. If tests are
blocked or an unrelated failure persists, report it and ask before publishing
with that limitation. Do not bypass tests or weaken assertions to obtain success.

## 4. Create the feature branch

After local testing, create the branch while carrying the uncommitted fix:

```bash
git switch -c "$BRANCH"
```

Confirm the branch name and that the new branch starts at the `main` SHA recorded
in step 0a. For an explicitly approved resume, skip branch creation, preserve its
history, and ensure steps 2 and 3 covered its resulting contents.

## 5. Commit and push

Review `git diff` and `git status`, stage only the intended paths, and inspect
`git diff --cached` and `git diff --cached --check` before committing.

Use a concise commit subject such as `[SELC-1234] Fix <specific behavior>`.
Follow repository commit conventions and include this trailer unless the user
explicitly requests otherwise:

```text
Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
```

Push the feature branch to the verified remote and set its upstream:

```bash
git push --set-upstream <remote> "$BRANCH"
```

If the push is rejected, inspect the reason; do not force-push or overwrite a
remote branch. Confirm the push succeeded before creating the PR.

## 6. Open the pull request

Read `.github/PULL_REQUEST_TEMPLATE.md` at execution time and use its actual
headings, sections, and checklists. Fill it with concrete, self-contained content;
do not replace it with a generic summary.

Explain the change and motivation, link the Jira ticket, and include exact test
commands, results, and any explicitly accepted limitations. Mark the bug-fix
checkbox and documentation checkboxes accurately. Use `Not applicable` for
sections such as screenshots when appropriate.

Create a PR against `main` with:

- Title: `[<TICKET-ID>] <TITLE OF THE FIX>`.
- Head: `feature/<TICKET-ID>`.
- Body: the completed repository template.

Use the configured GitHub MCP server's PR creation tool with explicit repository,
base, head, title, and body arguments. If this branch already has an open PR,
update it through MCP rather than creating a duplicate. Record the PR number and
URL.

## 7. Request Copilot code review

After opening or updating the PR, request **Copilot code review** on its latest
revision. Do not check, wait for, or repair post-PR workflows as part of this
skill.

Use the configured GitHub MCP server's dedicated Copilot review request tool
(`github-request_copilot_review` when exposed). Do not assign the ticket to the
Copilot coding agent, post an `@copilot` comment, or submit a review as the user:
these are not Copilot code-review requests.

Confirm GitHub accepted the request. If Copilot review is unavailable or denied,
report that explicitly. Requesting a review is not the same as receiving a
completed review.

Finish with the PR URL, a concise description of the fix, and any unresolved
blockers. Leave the PR open for human review.