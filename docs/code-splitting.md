# Code Splitting Rules

Split code by reason to change and ownership, not by arbitrary line count.

## Safe extraction order

1. Identify the behavior and its owner.
2. Define the public contract and dependency direction.
3. Extract the smallest cohesive unit.
4. Move tests with the responsibility.
5. Remove the old path and update documentation.
6. Verify architecture and behavior before adding another abstraction.

## Naming signals

- `Service` means application orchestration or a documented domain service.
- `Policy` or `Specification` means business decision logic.
- `Port` means an application-owned boundary.
- `Adapter` means a translation/implementation at an external boundary.
- `Entity`, `Value`, and `Aggregate` describe domain modeling roles.
- `Provider` is reserved for replaceable technology implementations in a
  capability or build boundary.

Avoid `Utils`, `Helpers`, `Manager`, `Common`, and `Misc` unless an ADR explains
why the name represents a real cohesive boundary.
