# Service CI Performance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Make emme-service CI run mutually independent verification jobs in parallel while exposing explicit manual inputs for expensive integration and image work.

**Architecture:** Keep one setup action per isolated GitHub job, remove unnecessary needs edges, co-locate unit tests and coverage, and reserve container/native image work for main/manual execution. Job summaries remain the blocking status contract.

**Tech Stack:** GitHub Actions, YAML, Gradle 9.4.1, Java 25, Docker Buildpacks, Trivy, Node.js 20 validators.

## Global Constraints

- Parallelize only jobs with no shared mutable workspace, database, Docker stack, port, artifact, or side effect.
- Pull requests must not build JVM or native images.
- Native-image compilation must remain manual-only and opt-in.
- publish must never be implied by selecting an image build.
- Coverage must not execute a second application test run on another runner.
- Existing architecture, formatting, dependency-verification, and security gates remain blocking.
- Do not introduce shell scripts for image creation.

---

## Task 1: Add service CI dispatch inputs and parallel job graph

**Files:**

- Modify: .github/workflows/ci-backend.yml
- Create: scripts/validate-backend-workflow.mjs
- Modify: tasks/todo.md

**Interfaces:**

- workflow_dispatch exposes run_integration, a boolean defaulting to true.
- quality, test, integration, build-logic, and infrastructure are independent jobs.
- coverageCheck runs from the test job; no separate coverage job remains.

- [ ] Step 1: Write the failing workflow-contract assertion.

Create scripts/validate-backend-workflow.mjs that reads .github/workflows/ci-backend.yml and requires these fragments:

    const required = [
      'run_integration:',
      'coverageCheck',
      'needs: [quality, test, integration, build-logic, infrastructure]',
      'if: always()',
    ];

It must also reject a separate coverage: job and reject needs: quality on the independent jobs.

- [ ] Step 2: Run the contract and verify it fails.

    node scripts/validate-backend-workflow.mjs

Expected: failure because the current workflow has no input, still has a coverage job, and serializes independent jobs.

- [ ] Step 3: Implement the workflow graph.

Add this dispatch input:

    workflow_dispatch:
      inputs:
        run_integration:
          description: Run Testcontainers integration tests
          required: true
          default: true
          type: boolean

Remove needs: quality from test, integration, build-logic, and infrastructure. Run tests and coverage together:

    - name: Run all backend unit, module, and coverage checks
      run: >-
        ./gradlew test :applications:emme-platform:coverageCheck
        --no-daemon --no-configuration-cache --stacktrace

Move JaCoCo artifact upload into test, delete coverage, and make integration conditional:

    if: github.event_name != 'workflow_dispatch' || inputs.run_integration == true

Make boot packaging wait for the independent jobs and accept a deliberately skipped manual integration job:

    if: >-
      always() && needs.quality.result == 'success' &&
      needs.test.result == 'success' &&
      needs.build-logic.result == 'success' &&
      needs.infrastructure.result == 'success' &&
      (needs.integration.result == 'success' || needs.integration.result == 'skipped')

Update the summary to require integration for normal events, while accepting skipped integration only for a manual run with run_integration=false.

- [ ] Step 4: Run the contract and repository validators.

    node scripts/validate-backend-workflow.mjs
    node scripts/validate-markdown.mjs
    git diff --check

Expected: all commands pass.

- [ ] Step 5: Commit.

    git add .github/workflows/ci-backend.yml scripts/validate-backend-workflow.mjs tasks/todo.md
    git commit -m "ci(service): parallelize backend verification jobs"

## Task 2: Make container execution event- and input-selectable

**Files:**

- Modify: .github/workflows/container-image.yml
- Modify: scripts/validate-container-workflow.mjs

**Interfaces:**

- Manual input jvm defaults to true.
- Manual inputs native and publish remain available.
- JVM images run on main pushes or selected manual dispatch, never pull requests.
- Native images run only on manual dispatch with native=true.

- [ ] Step 1: Extend the failing contract assertions.

Require jvm:, the JVM conditional expression, scanners: vuln, and limit-severities-for-sarif: true. Reject a pull_request: container trigger.

- [ ] Step 2: Run the validator and verify it fails.

    node scripts/validate-container-workflow.mjs

Expected: failure because the current workflow has no jvm input and still builds on pull requests.

- [ ] Step 3: Implement conditional image jobs.

Add:

    jvm:
      description: Build and scan the JVM image
      required: true
      default: true
      type: boolean

Remove pull_request: from the trigger and set the JVM job condition:

    if: github.event_name != 'workflow_dispatch' || inputs.jvm == true

Keep publication separately guarded by publish. Keep the explicit Trivy policy on both image jobs:

    scanners: vuln
    limit-severities-for-sarif: true
    severity: HIGH,CRITICAL
    ignore-unfixed: true
    exit-code: '1'

- [ ] Step 4: Run local workflow validation.

    node scripts/validate-container-workflow.mjs
    git diff --check

Expected: all commands pass.

- [ ] Step 5: Commit.

    git add .github/workflows/container-image.yml scripts/validate-container-workflow.mjs
    git commit -m "ci(service): make image jobs explicitly selectable"

## Task 3: Verify the service pipeline

**Files:**

- Modify: tasks/todo.md

- [ ] Step 1: Run local backend gates.

    ./gradlew spotlessCheck test :applications:emme-platform:coverageCheck
    ./gradlew ci -x test -x integrationTest -x e2eTest --no-daemon --no-configuration-cache
    node scripts/validate-backend-workflow.mjs
    node scripts/validate-container-workflow.mjs

Expected: all commands pass.

- [ ] Step 2: Dispatch the non-native image mode after pushing.

    gh workflow run container-image.yml \
      -R migangdelzar/emme-service \
      --ref feat/enterprise-module-template-conformance \
      -f jvm=true -f native=false -f publish=false

Verify the run contains the JVM job, excludes the native job, and applies the vulnerability-only scan policy.

- [ ] Step 3: Record verification and push.

Mark the CI performance section in tasks/todo.md complete only after local checks and the selected GitHub run finish successfully. Record canceled runs as canceled evidence, not successful evidence.

    git add tasks/todo.md
    git commit -m "docs(ci): record service pipeline verification"
    git push origin feat/enterprise-module-template-conformance
