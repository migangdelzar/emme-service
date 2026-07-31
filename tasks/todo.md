# Service architecture migration checklist

## Acceptance criteria

- [ ] CDD build-logic architecture is documented and remains active.
- [ ] Catalog persistence is separated from the pure domain model.
- [ ] Cross-module identity dependencies use public API/event contracts.
- [ ] Architecture tests pass without weakening boundary rules.
- [ ] Gradle dependency verification is complete for CI-resolved artifacts.
- [ ] Infrastructure manifests have a deterministic validation path.
- [ ] Web i18n follows the Clara reference pattern and its quality gate passes.
- [ ] Changes are committed and pushed in logical commits.

## Working notes

- The Modulith handbook is the source of truth for module and build-logic structure.
- `emme-service` already contains the CDD conventions and strict architecture tests;
  this task completes the implementation migration rather than creating a second
  architecture model.
- The web repository already has a shared `@emme/i18n` package; the migration will
  improve its type safety and locale boundary instead of introducing a duplicate
  package.

## Results

Pending implementation and verification.
