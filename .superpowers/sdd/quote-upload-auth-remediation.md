# Quote Upload Auth Remediation Verification

## Scoped changes

- Web conversation creation derives ownership from the authenticated JWT issuer and subject via `AiPrincipalIdentity`; the request's `participantId` is no longer used as the owner.
- Isolated controller coverage verifies that caller-supplied participant IDs cannot control ownership.
- Isolated MockMvc/proxy coverage verifies owner success, same-tenant other-principal denial, cross-tenant denial, and method-security denial for role, feature, and capability failures.
- The quote web fixture uses `@TestConfiguration`, and the shared test bootstrap applies Boot's `TypeExcludeFilter` to its explicit component scan so the fixture cannot leak into unrelated Spring contexts.

## Verification

The focused Java 25 run passed:

```text
mise exec java@25.0.2 -- ./gradlew :modules:assistant:test \
  --tests 'com.emme.assistant.adapter.in.web.controller.ConversationControllerTest' \
  --tests 'com.emme.assistant.ai.adapter.in.web.controller.DesignQuoteWebTest' \
  --no-daemon
```

Result: `BUILD SUCCESSFUL`; 7 tests passed.

## Relevant Spring-context verification

The previous `featureFlagService` bean collision is resolved: `ConversationWebTest` now fails later during application-context startup on the repository's existing missing `coreDataSource` configuration:

```text
No qualifying bean of type 'javax.sql.DataSource' available
```

The failing dependency is `AiJobExecutorConfiguration.coreJdbcTemplate`, which requires `coreDataSource`. This is unrelated to the quote fixture isolation and prevents the context test from reaching its test methods. The isolated authorization tests remain the executable verification for the quote endpoint in this worktree.
