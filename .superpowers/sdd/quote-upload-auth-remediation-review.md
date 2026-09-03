# Independent Review: Quote Upload Authorization Remediation

| Field | Value |
|---|---|
| Reviewed range | `cef29973..960585a6` |
| Target | `960585a6` (`fix(assistant): derive conversation ownership from authenticated identity`) |
| Prior review | `.superpowers/sdd/quote-upload-auth-review.md` |
| Remediation report | `.superpowers/sdd/quote-upload-auth-remediation.md` |
| Date | 2026-09-03 |

## Scope and conclusion

I independently reviewed the remediation report, prior review, target diff, assistant authorization path, tenant-scoped persistence path, API call sites, and added tests. The production authorization design now uses one canonical web ownership key: `AiPrincipalIdentity.fromTrustedClaims(jwt.issuer, jwt.subject)` is computed when a web conversation is created and the same derivation is used by the quote-upload execution context.

Tenant isolation, role enforcement, feature enforcement, and capability enforcement are fail-closed in the reviewed path. The request route, multipart parameters, and response field shape remain compatible; the caller-supplied `participantId` is retained for request compatibility but is no longer trusted as ownership input.

The package is not ready for approval because the added web test configuration leaks into unrelated Spring Boot tests and prevents their application contexts from loading.

## Findings

### F-01 — Medium: added test configuration breaks unrelated Spring-context tests

`modules/assistant/src/test/java/com/emme/assistant/ai/adapter/in/web/controller/DesignQuoteWebTest.java:268` declares the local fixture as `@Configuration`, and line 337 declares a bean named `featureFlagService`. Because test classes are on the component-scan classpath, this configuration is discovered by unrelated `@SpringBootTest` contexts. Those contexts already contain the production `@Service("featureFlagService")` (`FeatureFlagEvaluator`).

Fresh verification of the target worktree fails `ConversationWebTest` during context construction with:

```text
BeanDefinitionOverrideException:
... DesignQuoteWebTest$QuoteTestConfiguration ... bean 'featureFlagService'
... already ... FeatureFlagEvaluator ... bean 'featureFlagService'
```

The full module run reproduced the same collision across Spring-context tests (`453 tests completed, 18 failed`); other failures in that run are attributable to unrelated pre-existing worktree edits, but this bean collision is directly introduced by the target’s new test class. The remediation report incorrectly attributes the relevant context failure to a pre-existing `entityManagerFactory` problem.

Use `@TestConfiguration` for the local fixture (it remains directly registrable by the test’s `context.register(...)` call while being excluded from normal component scanning), or otherwise isolate/import the fixture only for `DesignQuoteWebTest`. Re-run the existing Spring-context tests after the correction.

## Authorization and compatibility verification

### Canonical ownership

- `ConversationController.start` derives the owner from the authenticated JWT at `ConversationController.java:82-88`.
- `AssistantWebMapper.toCommand` ignores the request participant and passes the derived owner at `AssistantWebMapper.java:12-15`.
- `DesignQuoteController.authorize` compares the context principal to the persisted conversation participant at `DesignQuoteController.java:120-127`.
- The derivation is the same `AiPrincipalIdentity` function in both paths, so the previous review’s ownership-key mismatch is remediated for web-created conversations.

### Tenant isolation

- Quote authorization requests `GetConversationQuery(context.tenantId(), conversationId)`.
- `GetConversationService` delegates to `findByTenantIdAndId`, and the persistence adapter uses `findByIdAndTenantId`.
- The controller also requires the returned conversation tenant to equal the backend context tenant before reading or storing image bytes.
- The added web tests cover same-tenant other-principal denial and cross-tenant denial.

### Role, feature, and capability enforcement

- The method boundary requires `tenant_client` or `client` and the `ai_chat` feature through `@PreAuthorize` at `DesignQuoteController.java:56-57`.
- The controller rechecks resolved `ai_chat`, `ai:basic`, and canonical client role membership before conversation lookup or storage at lines 110-119.
- The added proxy-level tests cover non-client role and disabled feature; the controller-level web test covers missing `ai:basic`.

### API compatibility

- `/api/conversations` and `/api/ai/quotes` routes are unchanged.
- Quote multipart parameter names and response shape are unchanged.
- `StartConversationRequest` still accepts the existing `participantId` field, but ownership is now server-derived as required by the remediation. Existing callers sending that field remain structurally compatible, while callers must no longer expect it to determine ownership.
- All repository call sites for the changed controller method and mapper signature were updated; no unresolved in-repository source compatibility break was found.

## Positive observations

- Authorization occurs before `MultipartFile.getBytes()`, storage, metadata persistence, workflow initialization, or quote processing.
- The ownership comparison fails closed when the conversation is absent, belongs to another tenant, or belongs to another principal.
- The focused test fixture invokes the Spring method-security proxy and verifies no downstream interactions for denied role, feature, capability, tenant, and principal cases.
- The production diff is localized to authenticated ownership derivation; no new client-controlled tenant or principal input was introduced.

## Verification performed

- `git pull --ff-only origin feat/ai-platform-foundation` — already up to date.
- `mise exec java@25.0.2 -- ./gradlew :modules:assistant:test --tests 'com.emme.assistant.adapter.in.web.controller.ConversationControllerTest' --tests 'com.emme.assistant.ai.adapter.in.web.controller.DesignQuoteWebTest' --no-daemon` — `BUILD SUCCESSFUL`; 7 tests passed (1 ownership test and 6 quote web tests).
- `mise exec java@25.0.2 -- ./gradlew :modules:assistant:test --tests 'com.emme.assistant.adapter.in.web.controller.ConversationWebTest' --no-daemon` — failed before test execution with the `featureFlagService` bean collision above.
- `mise exec java@25.0.2 -- ./gradlew :modules:assistant:test --no-daemon` — `453 tests completed, 18 failed`; the target-related Spring-context failures include the same bean collision. The worktree contains unrelated uncommitted source/test edits, so this run is not treated as a clean baseline.
- `git diff --check cef29973..960585a6` and `git show --check 960585a6` — clean.

## Clarification questions

None. The blocking issue is reproducible and has a contained remediation.

## Recommendation

**Needs revision** — F-01 is Medium and must be addressed before approval. Isolate the added test configuration, correct the remediation report’s claimed blocker, and re-run the affected Spring-context tests. Route the updated package back for review.
