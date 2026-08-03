# OpenAPI contract

The deployed `emme-platform` application publishes its HTTP contract at
`/api-docs`. The contract is versioned through the `API-Version` request header;
the current supported version is `1.0`. Public controller mappings remain
version-neutral (`/api/...`) and do not use legacy `/api/v1/...` aliases.

The reviewed route families are maintained in the executable manifest:

[`openapi-required-paths.txt`](../../applications/emme-platform/src/e2eTest/resources/contracts/openapi-required-paths.txt)

`OpenApiContractTest` fetches the deployed document and fails when the OpenAPI
document or any required route family disappears. The manifest is intentionally
small: it protects the stable integration surface without checking generated
descriptions or implementation-specific schema ordering. The separate
`ApiVersionConventionTest` protects the controller mapping declaration, while
`WebMvcConfiguration` protects the runtime resolver and default.

## Change policy

- Add a route to the manifest when it becomes a supported cross-client contract.
- Remove a route only with an explicit migration decision and a corresponding
  architecture or API decision record.
- Do not add `/api/v1` compatibility paths; version negotiation is header-based
  until a new version is deliberately introduced.
- External webhook callbacks are not included in this manifest because their
  versioning and signature contracts are owned by their external providers.
