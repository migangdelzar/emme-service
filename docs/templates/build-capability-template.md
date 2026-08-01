# Build Capability Template

Use this template for a non-trivial Gradle capability. Do not create empty
folders; materialize only responsibilities required by the capability.

```text
build-logic/src/main/kotlin/com/emme/buildlogic/<capability>/
├── Emme<Capability>Plugin.kt
├── Emme<Capability>Extension.kt
├── <Capability>Model.kt                 # only if capability-specific
├── task/
│   └── <Action>Task.kt
└── provider/
    ├── <Capability>Provider.kt           # port
    ├── <Capability>Result.kt
    └── <Technology>Provider.kt           # adapter
```

## Contract

- Convention plugin: `emme.<capability>.gradle.kts`.
- Binary plugin: `Emme<Capability>Plugin`.
- DSL extension: `Emme<Capability>Extension`.
- Task: verb-oriented `<Action>Task`.
- Provider port: `<Capability>Provider`.
- Provider adapter: `<Technology>Provider`.
- Result: `<Capability>Result`.

## Approval evidence

- [ ] The capability owns all files that change together.
- [ ] The extension is lazy configuration only.
- [ ] Tasks declare inputs and outputs with Gradle lazy APIs.
- [ ] External tools are behind provider ports.
- [ ] Unit tests cover selection and models.
- [ ] TestKit functional tests prove real plugin behavior.
- [ ] The capability is documented in `build-logic.md` and its README.
