# ADR-0009: Java 25 and Structured Concurrency

## Status

Proposed

## Date

2026-08-27

## Context

The repository declares Java 25, preview compilation, and Spring virtual
threads, but the AI workflow does not yet use `ScopedValue`,
`StructuredTaskScope`, Joiners, or explicit AI executors. Existing tenant and
correlation state uses ThreadLocal. A global ForkJoinPool override or custom
Thread subclass would make context, fairness, and testing harder.

## Decision

- Standardize the complete project on Java 25.
- Use immutable `AiExecutionContext` bound with `ScopedValue` at request/worker
  boundaries.
- Use `StructuredTaskScope` and Joiners for independent bounded reads.
- Hide preview APIs behind `ParallelTaskRunner`.
- Use named injected executors for non-structured work.
- Use virtual threads for blocking AI/HTTP operations and bounded platform
  executors for CPU-heavy work.
- Use Java agents for telemetry and diagnostics only.
- Do not globally replace `ForkJoinPool.commonPool()`.
- Do not extend `Thread` for application tasks.

## Consequences

Positive:

- Clear task lifetimes, cancellation, deadlines, and context inheritance.
- Explicit tenant/provider concurrency limits.
- Easier migration if the preview API changes.

Negative:

- Preview flags remain required while using Java 25 StructuredTaskScope.
- Context bridges are needed for ordinary executor and legacy ThreadLocal code.
- JVM Java-agent behavior needs a native-image alternative.

## Rejected alternatives

- Global ForkJoinPool replacement through a Java agent.
- Extending Thread for AI operations.
- Unbounded executor submission without tenant limits.
- Making ThreadLocal the only security or tenant source.
