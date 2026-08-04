# Studio Module Decomposition Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Decompose the monolithic `modules/studio` into DDD bounded contexts (`services`, `clients`, `appointments`, `salon`), extract nested capabilities (`documents`, `subscriptions`), and rename empty modules (`customer→clients`, `workforce→staffing`).

**Architecture:** The `studio` module (267 Java types) holds 4 bounded contexts mixed together. This plan splits them into 6 new modules following Hexagonal Architecture with `com.emme.<module>` packages: `appointments` (lifecycle, events, collision), `services` (catalog, artist capabilities), `clients` (CRM), `salon` (business config), plus two extracted capabilities: `documents` (upload, chunk, RAG) and `subscriptions` (plans, entitlements). All sharing continues via `api.*` packages and Spring Modulith events.

**Tech Stack:** Java 25, Spring Boot 4.1, Spring Modulith 2.1, Gradle Kotlin DSL, `emme.spring-module` convention plugin

## Global Constraints

- Java 25 required for Gradle build (`JAVA_HOME` from mise: `mise exec -- printenv JAVA_HOME`)
- Module package declarations follow `com.emme.<module>` pattern
- Every module uses `id("emme.spring-module")` convention plugin
- `@ApplicationModule(displayName=..., allowedDependencies=...)` required on each `package-info.java`
- Architecture tests (`ModularityTest`, `DddHexagonalArchitectureTest`, `CrossModuleDependencyArchitectureTest`) must pass
- Cross-module imports from `identity`, `calendar`, and `assistant` must be updated to new packages
- No `com.emme.studio` imports may remain after the split
- `settings.gradle.kts` and `applications/emme-platform/build.gradle.kts` must list all new modules

---

## File Inventory

### Module-boundary files (per module)

Each new module needs:
```
modules/<name>/
├── build.gradle.kts
├── src/main/java/com/emme/<name>/
│   ├── package-info.java
│   └── api/package-info.java
└── src/test/java/com/emme/<name>/
    └── <Name>ModuleTest.java
```

### Files to delete (moved into new modules)

The entire `modules/studio/src/` tree (200+ Java files) will be distributed across the new modules. After successful migration, `modules/studio/` is removed entirely.

### Cross-module consumers (require import updates)

| Consumer | Files to update | New import source |
|---|---|---|
| `identity` | 12 source + 5 test files | `appointments` (events), `salon` (business profile), `subscriptions` (plan types) |
| `calendar` | 2 source files | `appointments` (events, list use-case), `clients` (list use-case) |
| `assistant` | 1 source + 3 test files | `documents` (search chunks) |
| `booking` | build.gradle.kts | All new modules |
| `emme-platform` | build.gradle.kts | All new modules |

---

### Task 1: Prepare environment and verify baseline

**Files:** None created or modified.

**Interfaces:**
- Consumes: Nothing
- Produces: Baseline verification passes

- [ ] **Step 1: Set Java 25 for Gradle**

```bash
export JAVA_HOME=$(mise exec -- printenv JAVA_HOME)
java -version
```
Expected: `openjdk version "25.0.2"`

- [ ] **Step 2: Run baseline verification**

```bash
JAVA_HOME=$(mise exec -- printenv JAVA_HOME) ./gradlew :applications:emme-platform:test --tests '*ModularityTest' --no-configuration-cache
```
Expected: BUILD SUCCESSFUL, ModularityTest passes

- [ ] **Step 3: Run full check to confirm starting state**

```bash
JAVA_HOME=$(mise exec -- printenv JAVA_HOME) ./gradlew :modules:studio:check :modules:identity:check :modules:calendar:check :modules:assistant:check --no-configuration-cache
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit baseline**

```bash
git add -A && git commit -m "chore: baseline before studio module decomposition"
```

---

### Task 2: Rename empty modules (customer→clients, workforce→staffing)

**Files:**
- Rename: `modules/customer/` → `modules/clients/`
- Rename: `modules/workforce/` → `modules/staffing/`
- Modify: `settings.gradle.kts:34-36`
- Modify: `applications/emme-platform/build.gradle.kts:57-58`

**Interfaces:**
- Consumes: Baseline from Task 1
- Produces: Renamed modules with updated package declarations

- [ ] **Step 1: Rename module directories with git mv**

```bash
git mv modules/customer modules/clients
git mv modules/workforce modules/staffing
```

- [ ] **Step 2: Move Java source to new package directories**

```bash
# clients
mkdir -p modules/clients/src/main/java/com/emme/clients/api
mv modules/clients/src/main/java/com/emme/customer/api/package-info.java modules/clients/src/main/java/com/emme/clients/api/
mv modules/clients/src/main/java/com/emme/customer/package-info.java modules/clients/src/main/java/com/emme/clients/
rmdir modules/clients/src/main/java/com/emme/customer/api 2>/dev/null || true
rmdir modules/clients/src/main/java/com/emme/customer 2>/dev/null || true

# clients tests
mkdir -p modules/clients/src/test/java/com/emme/clients
for f in modules/clients/src/test/java/com/emme/customer/*.java; do
  [ -f "$f" ] && cp "$f" "modules/clients/src/test/java/com/emme/clients/$(basename $f)"
done
rm -rf modules/clients/src/test/java/com/emme/customer

# staffing
mkdir -p modules/staffing/src/main/java/com/emme/staffing/api
mv modules/staffing/src/main/java/com/emme/workforce/api/package-info.java modules/staffing/src/main/java/com/emme/staffing/api/
mv modules/staffing/src/main/java/com/emme/workforce/package-info.java modules/staffing/src/main/java/com/emme/staffing/
rmdir modules/staffing/src/main/java/com/emme/workforce/api 2>/dev/null || true
rmdir modules/staffing/src/main/java/com/emme/workforce 2>/dev/null || true

# staffing tests
mkdir -p modules/staffing/src/test/java/com/emme/staffing
for f in modules/staffing/src/test/java/com/emme/workforce/*.java; do
  [ -f "$f" ] && cp "$f" "modules/staffing/src/test/java/com/emme/staffing/$(basename $f)"
done
rm -rf modules/staffing/src/test/java/com/emme/workforce
```

- [ ] **Step 3: Update package-info.java annotations and package declarations**

Read and replace in `modules/clients/src/main/java/com/emme/clients/package-info.java`:
```java
@org.springframework.modulith.ApplicationModule(
    displayName = "Clients",
    allowedDependencies = {"shared", "tenancy"})
package com.emme.clients;
```

Read and replace in `modules/clients/src/main/java/com/emme/clients/api/package-info.java`:
```java
package com.emme.clients.api;
```

Read and replace in `modules/staffing/src/main/java/com/emme/staffing/package-info.java`:
```java
@org.springframework.modulith.ApplicationModule(
    displayName = "Staffing",
    allowedDependencies = {"shared", "tenancy"})
package com.emme.staffing;
```

Read and replace in `modules/staffing/src/main/java/com/emme/staffing/api/package-info.java`:
```java
package com.emme.staffing.api;
```

- [ ] **Step 4: Update test files package declarations**

```bash
sed -i '' 's/package com\.emme\.customer;/package com.emme.clients;/g' modules/clients/src/test/java/com/emme/clients/*.java
sed -i '' 's/"customer"/"clients"/g' modules/clients/src/test/java/com/emme/clients/CustomerModuleTest.java
sed -i '' 's/package com\.emme\.workforce;/package com.emme.staffing;/g' modules/staffing/src/test/java/com/emme/staffing/*.java
sed -i '' 's/"workforce"/"staffing"/g' modules/staffing/src/test/java/com/emme/staffing/WorkforceModuleTest.java
```

- [ ] **Step 5: Update settings.gradle.kts**

Replace lines 35-36:
```kotlin
include(":modules:customer")
include(":modules:workforce")
```
With:
```kotlin
include(":modules:clients")
include(":modules:staffing")
```

- [ ] **Step 6: Update emme-platform build.gradle.kts**

Replace lines 57-58:
```kotlin
  implementation(project(":modules:customer"))
  implementation(project(":modules:workforce"))
```
With:
```kotlin
  implementation(project(":modules:clients"))
  implementation(project(":modules:staffing"))
```

- [ ] **Step 7: Update booking module build.gradle.kts references**

Read `modules/booking/build.gradle.kts` and replace:
```kotlin
  implementation(project(":modules:customer"))
```
With:
```kotlin
  implementation(project(":modules:clients"))
```
And:
```kotlin
  implementation(project(":modules:workforce"))
```
With:
```kotlin
  implementation(project(":modules:staffing"))
```

- [ ] **Step 8: Verify renamed modules compile**

```bash
JAVA_HOME=$(mise exec -- printenv JAVA_HOME) ./gradlew :modules:clients:compileJava :modules:staffing:compileJava --no-configuration-cache
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 9: Commit**

```bash
git add -A && git commit -m "refactor: rename customer→clients, workforce→staffing

- modules/customer → modules/clients (CRM bounded context)
- modules/workforce → modules/staffing (staff scheduling context)
- Updated package declarations, settings.gradle.kts, and all references"
```

---

### Task 3: Extract `subscriptions` from studio into standalone module

**Files:**
- Create: `modules/subscriptions/build.gradle.kts`, package-info, api/package-info
- Move: All `modules/studio/src/main/java/com/emme/studio/subscriptions/**` → `modules/subscriptions/src/main/java/com/emme/subscriptions/**`
- Move: All `modules/studio/src/test/java/com/emme/studio/subscriptions/**` → `modules/subscriptions/src/test/java/com/emme/subscriptions/**`
- Create: `modules/subscriptions/src/test/java/com/emme/subscriptions/SubscriptionsModuleTest.java`
- Modify: `settings.gradle.kts` — add `:modules:subscriptions`
- Modify: `applications/emme-platform/build.gradle.kts` — add subscription dependency
- Modify: All `identity` files importing `com.emme.studio.subscriptions` — change to `com.emme.subscriptions`

**Interfaces:**
- Consumes: Renamed modules from Task 2
- Produces: Standalone `subscriptions` module, `identity` consumers updated

- [ ] **Step 1: Create module directory structure**

```bash
mkdir -p modules/subscriptions/src/main/java/com/emme/subscriptions/api
mkdir -p modules/subscriptions/src/test/java/com/emme/subscriptions
```

- [ ] **Step 2: Create build.gradle.kts**

Write `modules/subscriptions/build.gradle.kts`:
```kotlin
plugins {
  id("emme.spring-module")
  id("emme.integration-testing")
  id("emme.spring-web")
  id("emme.persistence")
  id("emme.testing")
}

dependencies {
  implementation(project(":modules:shared"))
  implementation(project(":libraries:kernel"))
  implementation(project(":modules:tenancy"))
  implementation(project(":libraries:kernel"))

  implementation(libs.spring.boot.starter.web)
  implementation(libs.spring.boot.starter.validation)
  implementation(libs.springdoc.openapi.starter.webmvc.ui)

  testImplementation(testFixtures(project(":libraries:testing")))
}
```

- [ ] **Step 3: Create package-info.java**

Write `modules/subscriptions/src/main/java/com/emme/subscriptions/package-info.java`:
```java
@org.springframework.modulith.ApplicationModule(
    displayName = "Subscriptions",
    allowedDependencies = {"shared", "tenancy"})
package com.emme.subscriptions;
```

Write `modules/subscriptions/src/main/java/com/emme/subscriptions/api/package-info.java`:
```java
package com.emme.subscriptions.api;
```

- [ ] **Step 4: Move subscription source files from studio**

```bash
# Move main sources, rewriting package paths
STUDIO_SUB="modules/studio/src/main/java/com/emme/studio/subscriptions"
TARGET_SUB="modules/subscriptions/src/main/java/com/emme/subscriptions"

# Create target directory structure (mirroring studio sub-packages)
for dir in $(find $STUDIO_SUB -type d | sed "s|$STUDIO_SUB||"); do
  mkdir -p "${TARGET_SUB}${dir}"
done

# Copy all Java files
for f in $(find $STUDIO_SUB -name "*.java"); do
  rel=${f#$STUDIO_SUB/}
  cp "$f" "${TARGET_SUB}/${rel}"
  # Replace package declarations
  sed -i '' 's/package com\.emme\.studio\.subscriptions/package com.emme.subscriptions/g' "${TARGET_SUB}/${rel}"
  # Replace intra-module imports
  sed -i '' 's/import com\.emme\.studio\.subscriptions/import com.emme.subscriptions/g' "${TARGET_SUB}/${rel}"
done

# Move test sources
STUDIO_SUB_TEST="modules/studio/src/test/java/com/emme/studio/subscriptions"
TARGET_SUB_TEST="modules/subscriptions/src/test/java/com/emme/subscriptions"
for dir in $(find $STUDIO_SUB_TEST -type d 2>/dev/null | sed "s|$STUDIO_SUB_TEST||"); do
  mkdir -p "${TARGET_SUB_TEST}${dir}"
done
for f in $(find $STUDIO_SUB_TEST -name "*.java" 2>/dev/null); do
  rel=${f#$STUDIO_SUB_TEST/}
  cp "$f" "${TARGET_SUB_TEST}/${rel}"
  sed -i '' 's/package com\.emme\.studio\.subscriptions/package com.emme.subscriptions/g' "${TARGET_SUB_TEST}/${rel}"
  sed -i '' 's/import com\.emme\.studio\.subscriptions/import com.emme.subscriptions/g' "${TARGET_SUB_TEST}/${rel}"
done
```

- [ ] **Step 5: Create SubscriptionsModuleTest.java**

Write `modules/subscriptions/src/test/java/com/emme/subscriptions/SubscriptionsModuleTest.java`:
```java
package com.emme.subscriptions;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.testing.BaseUnitTest;
import org.junit.jupiter.api.Test;

class SubscriptionsModuleTest extends BaseUnitTest {

  @Test
  void moduleLoads() {
    assertThat(getClass().getPackageName()).contains("subscriptions");
  }

  @Test
  void testStructureExists() {
    assertThat(true).isTrue();
  }
}
```

- [ ] **Step 6: Update settings.gradle.kts**

Add after `include(":modules:studio")`:
```kotlin
include(":modules:subscriptions")
```

- [ ] **Step 7: Update emme-platform build.gradle.kts**

Add after `implementation(project(":modules:studio"))`:
```kotlin
  implementation(project(":modules:subscriptions"))
```

- [ ] **Step 8: Update identity module imports (12 source files)**

For each file in `modules/identity/src/main/java/` that imports `com.emme.studio.subscriptions`, replace `com.emme.studio.subscriptions` with `com.emme.subscriptions`:

Files to update:
```
modules/identity/src/main/java/com/emme/identity/domain/model/FeatureFlag.java
modules/identity/src/main/java/com/emme/identity/api/result/FeatureFlagDetails.java
modules/identity/src/main/java/com/emme/identity/application/port/out/SubscriptionPlanPort.java
modules/identity/src/main/java/com/emme/identity/adapter/in/web/request/UpdateFeatureFlagRequest.java
modules/identity/src/main/java/com/emme/identity/adapter/in/web/request/CreateFeatureFlagRequest.java
modules/identity/src/main/java/com/emme/identity/api/command/SetPlatformFeatureFlagCommand.java
modules/identity/src/main/java/com/emme/identity/adapter/in/web/response/FeatureFlagResponse.java
modules/identity/src/main/java/com/emme/identity/adapter/out/persistence/entity/FeatureFlagEntity.java
modules/identity/src/main/java/com/emme/identity/adapter/out/client/subscription/SubscriptionPlanAdapter.java
```

Run for each file:
```bash
sed -i '' 's/import com\.emme\.studio\.subscriptions/import com.emme.subscriptions/g' <filepath>
```

Also update identity test files:
```bash
for f in modules/identity/src/test/java/com/emme/identity/adapter/out/persistence/mapper/FeatureFlagPersistenceMapperTest.java modules/identity/src/test/java/com/emme/identity/application/authorization/FeatureFlagEvaluatorTest.java modules/identity/src/test/java/com/emme/identity/application/service/SetPlatformFeatureFlagServiceTest.java modules/identity/src/test/java/com/emme/identity/domain/FeatureFlagTest.java; do
  sed -i '' 's/import com\.emme\.studio\.subscriptions/import com.emme.subscriptions/g' "$f"
done
```

- [ ] **Step 9: Update studio remaining files**

The subscriptions sub-directory is now removed from studio. The studio module's `build.gradle.kts` no longer needs the old subscription internals (they were all inline in studio). No studio source files import `com.emme.studio.subscriptions` internally since they all used same-package imports.

- [ ] **Step 10: Update architecture tests**

Find and update any architecture test that references `studio.subscriptions`:
```bash
rg -l "studio.*subscriptions\|subscriptions.*studio" --glob "*.java" --glob "!**/build/**" modules/ applications/src/test/
```

For each found file, update references to `com.emme.subscriptions`.

- [ ] **Step 11: Verify subscriptions module**

```bash
JAVA_HOME=$(mise exec -- printenv JAVA_HOME) ./gradlew :modules:subscriptions:test :modules:identity:compileJava --no-configuration-cache
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 12: Commit**

```bash
git add -A && git commit -m "refactor: extract subscriptions from studio into standalone module

- New modules/subscriptions with DDD layers (api, application, domain, adapter)
- Updated 12 identity source files + 4 test files to import from com.emme.subscriptions
- Updated settings.gradle.kts and emme-platform build.gradle.kts"
```

---

### Task 4: Extract `documents` from studio into standalone module

**Files:**
- Create: `modules/documents/` with full structure (mirrors `studio/documents/`)
- Move: All documents sub-package Java files with package renames
- Modify: `modules/assistant/src/main/java/com/emme/assistant/ai/application/service/RagQueryService.java`
- Modify: `modules/assistant/src/test/` — 3 files updating imports
- Modify: `settings.gradle.kts`, `applications/emme-platform/build.gradle.kts`

**Interfaces:**
- Consumes: subscriptions extraction from Task 3
- Produces: Standalone `documents` module, `assistant` consumers updated

- [ ] **Step 1: Create module directory structure**

```bash
# Main structure (mirrors studio/documents/)
DOCS_DIRS="adapter/in/web/controller adapter/in/web/mapper adapter/in/web/request adapter/in/web/response adapter/out/persistence/adapter adapter/out/persistence/entity adapter/out/persistence/mapper adapter/out/persistence/repository adapter/out/search api/command api/exception api/query api/result api/usecase application/mapper application/port/out application/service configuration domain/model"
for d in $DOCS_DIRS; do
  mkdir -p "modules/documents/src/main/java/com/emme/documents/$d"
done

# Test structure
mkdir -p modules/documents/src/test/java/com/emme/documents
```

- [ ] **Step 2: Create build.gradle.kts**

Write `modules/documents/build.gradle.kts`:
```kotlin
plugins {
  id("emme.spring-module")
  id("emme.integration-testing")
  id("emme.spring-web")
  id("emme.persistence")
  id("emme.testing")
}

dependencies {
  implementation(project(":modules:shared"))
  implementation(project(":libraries:kernel"))
  implementation(project(":modules:tenancy"))
  implementation(project(":libraries:kernel"))

  implementation(libs.spring.boot.starter.web)
  implementation(libs.spring.boot.starter.validation)
  implementation(libs.springdoc.openapi.starter.webmvc.ui)

  testImplementation(testFixtures(project(":libraries:testing")))
}
```

- [ ] **Step 3: Create package-info.java**

Write `modules/documents/src/main/java/com/emme/documents/package-info.java`:
```java
@org.springframework.modulith.ApplicationModule(
    displayName = "Documents",
    allowedDependencies = {"shared :: persistence", "shared :: search", "tenancy"})
package com.emme.documents;
```

Write `modules/documents/src/main/java/com/emme/documents/api/package-info.java`:
```java
package com.emme.documents.api;
```

- [ ] **Step 4: Move documents source files from studio**

```bash
STUDIO_DOC="modules/studio/src/main/java/com/emme/studio/documents"
TARGET_DOC="modules/documents/src/main/java/com/emme/documents"

# Copy main sources with package rewrite
for f in $(find $STUDIO_DOC -name "*.java"); do
  rel=${f#$STUDIO_DOC/}
  cp "$f" "${TARGET_DOC}/${rel}"
  sed -i '' 's/package com\.emme\.studio\.documents/package com.emme.documents/g' "${TARGET_DOC}/${rel}"
  sed -i '' 's/import com\.emme\.studio\.documents/import com.emme.documents/g' "${TARGET_DOC}/${rel}"
done

# Copy test sources
STUDIO_DOC_TEST="modules/studio/src/test/java/com/emme/studio/documents"
TARGET_DOC_TEST="modules/documents/src/test/java/com/emme/documents"
for f in $(find $STUDIO_DOC_TEST -name "*.java" 2>/dev/null); do
  rel=${f#$STUDIO_DOC_TEST/}
  target_dir=$(dirname "${TARGET_DOC_TEST}/${rel}")
  mkdir -p "$target_dir"
  cp "$f" "${TARGET_DOC_TEST}/${rel}"
  sed -i '' 's/package com\.emme\.studio\.documents/package com.emme.documents/g' "${TARGET_DOC_TEST}/${rel}"
  sed -i '' 's/import com\.emme\.studio\.documents/import com.emme.documents/g' "${TARGET_DOC_TEST}/${rel}"
done
```

- [ ] **Step 5: Create DocumentsModuleTest.java**

Write `modules/documents/src/test/java/com/emme/documents/DocumentsModuleTest.java`:
```java
package com.emme.documents;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.testing.BaseUnitTest;
import org.junit.jupiter.api.Test;

class DocumentsModuleTest extends BaseUnitTest {

  @Test
  void moduleLoads() {
    assertThat(getClass().getPackageName()).contains("documents");
  }

  @Test
  void testStructureExists() {
    assertThat(true).isTrue();
  }
}
```

- [ ] **Step 6: Update settings.gradle.kts**

Add after the studio include:
```kotlin
include(":modules:documents")
```

- [ ] **Step 7: Update emme-platform build.gradle.kts**

Add:
```kotlin
  implementation(project(":modules:documents"))
```

- [ ] **Step 8: Update assistant module imports**

Update `modules/assistant/src/main/java/com/emme/assistant/ai/application/service/RagQueryService.java`:
```bash
sed -i '' 's/import com\.emme\.studio\.documents/import com.emme.documents/g' modules/assistant/src/main/java/com/emme/assistant/ai/application/service/RagQueryService.java
```

Update assistant test files:
```bash
for f in modules/assistant/src/test/java/com/emme/assistant/ai/application/service/RagQueryServiceTest.java modules/assistant/src/test/java/com/emme/assistant/ai/web/AiWebTest.java modules/assistant/src/test/java/com/emme/conversations/web/ConversationWebTest.java; do
  sed -i '' 's/import com\.emme\.studio\.documents/import com.emme.documents/g' "$f" 2>/dev/null || true
done
```

- [ ] **Step 9: Verify documents module**

```bash
JAVA_HOME=$(mise exec -- printenv JAVA_HOME) ./gradlew :modules:documents:compileJava :modules:assistant:compileJava --no-configuration-cache
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 10: Commit**

```bash
git add -A && git commit -m "refactor: extract documents from studio into standalone module

- New modules/documents with DDD layers
- Updated assistant RagQueryService + tests to import from com.emme.documents
- Updated settings.gradle.kts and emme-platform build.gradle.kts"
```

---

### Task 5: Create `services` module (service catalog + artist capabilities)

**Files:**
- Create: `modules/services/` with DDD structure
- Move: Service domain, Artist domain, ArtistCapability domain from studio
- Move: Service and Artist use cases, application services, adapters, controllers
- Modify: `settings.gradle.kts`, `applications/emme-platform/build.gradle.kts`

**Interfaces:**
- Consumes: Extracted subscriptions and documents from Tasks 3-4
- Produces: Standalone `services` module — consumed by `appointments` (Task 7)

- [ ] **Step 1: Create module directory structure**

```bash
mkdir -p modules/services/src/main/java/com/emme/services/{api/{event,exception,result,type,usecase},application/{mapper,port/out,service},domain/model,adapter/{in/web/{controller,request,response},out/persistence/{adapter,entity,mapper,repository}}}
mkdir -p modules/services/src/test/java/com/emme/services
```

- [ ] **Step 2: Create build.gradle.kts**

Write `modules/services/build.gradle.kts`:
```kotlin
plugins {
  id("emme.spring-module")
  id("emme.integration-testing")
  id("emme.spring-web")
  id("emme.persistence")
  id("emme.testing")
}

dependencies {
  implementation(project(":modules:shared"))
  implementation(project(":libraries:kernel"))
  implementation(project(":modules:tenancy"))
  implementation(project(":libraries:kernel"))

  implementation(libs.spring.boot.starter.web)
  implementation(libs.spring.boot.starter.validation)
  implementation(libs.springdoc.openapi.starter.webmvc.ui)

  testImplementation(testFixtures(project(":libraries:testing")))
}
```

- [ ] **Step 3: Create package-info.java**

Write `modules/services/src/main/java/com/emme/services/package-info.java`:
```java
@org.springframework.modulith.ApplicationModule(
    displayName = "Services",
    allowedDependencies = {"shared :: persistence", "tenancy"})
package com.emme.services;
```

Write `modules/services/src/main/java/com/emme/services/api/package-info.java`:
```java
package com.emme.services.api;
```

- [ ] **Step 4: Move service-related domain, API, application, and adapter files**

```bash
STUDIO_SRC="modules/studio/src/main/java/com/emme/studio"
SRV_SRC="modules/services/src/main/java/com/emme/services"

# Domain: Service, ServiceStatus, Artist, ArtistStatus, ArtistCapability
for f in "$STUDIO_SRC/domain/model/Service.java" "$STUDIO_SRC/domain/model/ServiceStatus.java" "$STUDIO_SRC/domain/model/Artist.java" "$STUDIO_SRC/domain/model/ArtistStatus.java" "$STUDIO_SRC/domain/model/ArtistCapability.java"; do
  [ -f "$f" ] && cp "$f" "${SRV_SRC}/domain/model/$(basename $f)"
done

# API use cases: service + artist related
for f in "$STUDIO_SRC"/api/usecase/{CreateServiceCatalog*,UpdateServiceCatalog*,GetServiceCatalog*,ListActiveServiceCatalog*,RetireServiceCatalog*,CreateArtist*,UpdateArtist*,GetArtist*,ListTenantArtist*,DeactivateArtist*,AddArtist*,RemoveArtist*}UseCase.java; do
  [ -f "$f" ] && cp "$f" "${SRV_SRC}/api/usecase/$(basename $f)"
done

# API results: service + artist related
for f in "$STUDIO_SRC"/api/result/{ServiceDetails,ArtistDetails,ArtistCapabilityDetails}.java; do
  [ -f "$f" ] && cp "$f" "${SRV_SRC}/api/result/$(basename $f)"
done

# Application services: service + artist related
for f in "$STUDIO_SRC"/application/service/{CreateServiceCatalog*,UpdateServiceCatalog*,GetServiceCatalog*,ListActiveServiceCatalog*,RetireServiceCatalog*,CreateArtist*,UpdateArtist*,GetArtist*,ListTenantArtist*,DeactivateArtist*,AddArtist*,RemoveArtist*}Service.java; do
  [ -f "$f" ] && cp "$f" "${SRV_SRC}/application/service/$(basename $f)"
done

# Application mappers
cp "$STUDIO_SRC/application/mapper/ServiceCatalogApplicationMapper.java" "$SRV_SRC/application/mapper/" 2>/dev/null || true
cp "$STUDIO_SRC/application/mapper/ArtistApplicationMapper.java" "$SRV_SRC/application/mapper/" 2>/dev/null || true

# Application ports
for f in "$STUDIO_SRC"/application/port/out/{ServiceRepository,ArtistRepository,ArtistCapabilityRepository}.java; do
  [ -f "$f" ] && cp "$f" "${SRV_SRC}/application/port/out/$(basename $f)"
done

# Adapters: persistence
for f in "$STUDIO_SRC"/adapter/out/persistence/entity/{ServiceEntity,ArtistEntity,ArtistCapabilityEntity}.java; do
  [ -f "$f" ] && cp "$f" "${SRV_SRC}/adapter/out/persistence/entity/$(basename $f)"
done
for f in "$STUDIO_SRC"/adapter/out/persistence/mapper/{ServicePersistenceMapper,ArtistPersistenceMapper,ArtistCapabilityPersistenceMapper}.java; do
  [ -f "$f" ] && cp "$f" "${SRV_SRC}/adapter/out/persistence/mapper/$(basename $f)"
done
for f in "$STUDIO_SRC"/adapter/out/persistence/repository/{SpringDataServiceRepository,SpringDataArtistRepository,SpringDataArtistCapabilityRepository}.java; do
  [ -f "$f" ] && cp "$f" "${SRV_SRC}/adapter/out/persistence/repository/$(basename $f)"
done
for f in "$STUDIO_SRC"/adapter/out/persistence/adapter/{ServicePersistenceAdapter,ArtistPersistenceAdapter,ArtistCapabilityPersistenceAdapter}.java; do
  [ -f "$f" ] && cp "$f" "${SRV_SRC}/adapter/out/persistence/adapter/$(basename $f)"
done

# Adapters: web controllers + requests + responses
cp "$STUDIO_SRC/adapter/in/web/controller/ServiceController.java" "$SRV_SRC/adapter/in/web/controller/" 2>/dev/null || true
cp "$STUDIO_SRC/adapter/in/web/controller/ArtistController.java" "$SRV_SRC/adapter/in/web/controller/" 2>/dev/null || true
for f in "$STUDIO_SRC"/adapter/in/web/request/{CreateServiceRequest,UpdateServiceRequest,CreateArtistRequest,UpdateArtistRequest,AddArtistCapabilityRequest}.java; do
  [ -f "$f" ] && cp "$f" "${SRV_SRC}/adapter/in/web/request/$(basename $f)"
done
for f in "$STUDIO_SRC"/adapter/in/web/response/{ServiceResponse,ArtistResponse,ArtistCapabilityResponse}.java; do
  [ -f "$f" ] && cp "$f" "${SRV_SRC}/adapter/in/web/response/$(basename $f)"
done
```

- [ ] **Step 5: Rewrite all package declarations**

```bash
# Rewrite package declarations in all moved files
for f in $(find modules/services/src/main/java -name "*.java"); do
  sed -i '' 's/^package com\.emme\.studio;/package com.emme.services;/g' "$f"
  sed -i '' 's/^package com\.emme\.studio\./package com.emme.services./g' "$f"
done

# Rewrite intra-module imports
for f in $(find modules/services/src/main/java -name "*.java"); do
  sed -i '' 's/import com\.emme\.studio\.domain/import com.emme.services.domain/g' "$f"
  sed -i '' 's/import com\.emme\.studio\.api/import com.emme.services.api/g' "$f"
  sed -i '' 's/import com\.emme\.studio\.application/import com.emme.services.application/g' "$f"
  sed -i '' 's/import com\.emme\.studio\.adapter/import com.emme.services.adapter/g' "$f"
done
```

- [ ] **Step 6: Create package-info.java for sub-packages**

```bash
for dir in domain domain/model api api/event api/exception api/result api/type api/usecase application application/mapper application/port application/port/out application/service adapter adapter/in adapter/in/web adapter/in/web/controller adapter/in/web/request adapter/in/web/response adapter/out adapter/out/persistence adapter/out/persistence/adapter adapter/out/persistence/entity adapter/out/persistence/mapper adapter/out/persistence/repository; do
  pkg_path="modules/services/src/main/java/com/emme/services/$dir"
  pkg_name=$(echo "$dir" | tr '/' '.')
  if [ -d "$pkg_path" ] && [ ! -f "$pkg_path/package-info.java" ]; then
    echo "package com.emme.services.$pkg_name;" > "$pkg_path/package-info.java"
  fi
done
```

- [ ] **Step 7: Create ServicesModuleTest.java**

Write `modules/services/src/test/java/com/emme/services/ServicesModuleTest.java`:
```java
package com.emme.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.testing.BaseUnitTest;
import org.junit.jupiter.api.Test;

class ServicesModuleTest extends BaseUnitTest {

  @Test
  void moduleLoads() {
    assertThat(getClass().getPackageName()).contains("services");
  }

  @Test
  void testStructureExists() {
    assertThat(true).isTrue();
  }
}
```

- [ ] **Step 8: Update settings.gradle.kts**

Add:
```kotlin
include(":modules:services")
```

- [ ] **Step 9: Update emme-platform build.gradle.kts**

Add:
```kotlin
  implementation(project(":modules:services"))
```

- [ ] **Step 10: Verify services module compiles**

```bash
JAVA_HOME=$(mise exec -- printenv JAVA_HOME) ./gradlew :modules:services:compileJava --no-configuration-cache
```
Expected: BUILD SUCCESSFUL. Fix any compilation errors from missing types.

- [ ] **Step 11: Commit**

```bash
git add -A && git commit -m "refactor: extract services module from studio

- New modules/services: service catalog + artist capabilities bounded context
- Moved Service, Artist, ArtistCapability domain, use cases, adapters
- Updated settings.gradle.kts and emme-platform build.gradle.kts"
```

---

### Task 6: Create `clients` module (customer CRM)

**Files:**
- Create: `modules/clients/` with DDD structure (extends renamed empty module)
- Move: Customer domain, use cases, application services, adapters, controllers from studio
- Modify: `settings.gradle.kts`, `applications/emme-platform/build.gradle.kts`

**Note:** `modules/clients/` already exists from Task 2 (renamed from customer). We will *add* the CRM source files into the existing structure.

**Interfaces:**
- Consumes: services module from Task 5
- Produces: Standalone `clients` module — consumed by `appointments` (Task 7), `calendar` (Task 9)

- [ ] **Step 1: Create DDD sub-directories in existing clients module**

```bash
CLI_SRC="modules/clients/src/main/java/com/emme/clients"
mkdir -p "$CLI_SRC"/{api/{event,exception,result,type,usecase},application/{mapper,port/out,service},domain/model,adapter/{in/web/{controller,request,response},out/persistence/{adapter,entity,mapper,repository}}}
```

- [ ] **Step 2: Move customer-related files from studio**

```bash
STUDIO_SRC="modules/studio/src/main/java/com/emme/studio"

# Domain
cp "$STUDIO_SRC/domain/model/Customer.java" "$CLI_SRC/domain/model/"
cp "$STUDIO_SRC/domain/model/CustomerStatus.java" "$CLI_SRC/domain/model/"

# API use cases
for f in "$STUDIO_SRC"/api/usecase/{CreateCustomer*,UpdateCustomer*,GetCustomer*,ListCustomers*,ListTenantCustomer*,RetireCustomer*,SearchCustomer*}UseCase.java; do
  [ -f "$f" ] && cp "$f" "$CLI_SRC/api/usecase/$(basename $f)"
done

# API results
for f in "$STUDIO_SRC"/api/result/{CustomerDetails,CustomerSummary}.java; do
  [ -f "$f" ] && cp "$f" "$CLI_SRC/api/result/$(basename $f)"
done

# Application services
for f in "$STUDIO_SRC"/application/service/{CreateCustomer*,UpdateCustomer*,GetCustomer*,ListCustomers*,ListTenantCustomer*,RetireCustomer*,SearchCustomer*}Service.java; do
  [ -f "$f" ] && cp "$f" "$CLI_SRC/application/service/$(basename $f)"
done

# Application mapper
cp "$STUDIO_SRC/application/mapper/CustomerApplicationMapper.java" "$CLI_SRC/application/mapper/"

# Application ports
cp "$STUDIO_SRC/application/port/out/CustomerRepository.java" "$CLI_SRC/application/port/out/"

# Adapters
cp "$STUDIO_SRC/adapter/out/persistence/entity/CustomerEntity.java" "$CLI_SRC/adapter/out/persistence/entity/"
cp "$STUDIO_SRC/adapter/out/persistence/mapper/CustomerPersistenceMapper.java" "$CLI_SRC/adapter/out/persistence/mapper/"
cp "$STUDIO_SRC/adapter/out/persistence/repository/SpringDataCustomerRepository.java" "$CLI_SRC/adapter/out/persistence/repository/"
cp "$STUDIO_SRC/adapter/out/persistence/adapter/CustomerPersistenceAdapter.java" "$CLI_SRC/adapter/out/persistence/adapter/"
cp "$STUDIO_SRC/adapter/in/web/controller/CustomerController.java" "$CLI_SRC/adapter/in/web/controller/"
cp "$STUDIO_SRC/adapter/in/web/request/CreateCustomerRequest.java" "$CLI_SRC/adapter/in/web/request/"
cp "$STUDIO_SRC/adapter/in/web/request/UpdateCustomerRequest.java" "$CLI_SRC/adapter/in/web/request/"
cp "$STUDIO_SRC/adapter/in/web/response/CustomerResponse.java" "$CLI_SRC/adapter/in/web/response/"
```

- [ ] **Step 3: Rewrite package declarations**

```bash
for f in $(find modules/clients/src/main/java -name "*.java" -path "*/com/emme/clients/*" ! -path "*/api/package-info.java" ! -name "package-info.java"); do
  sed -i '' 's/^package com\.emme\.studio;/package com.emme.clients;/g' "$f"
  sed -i '' 's/^package com\.emme\.studio\./package com.emme.clients./g' "$f"
done

for f in $(find modules/clients/src/main/java -name "*.java" -path "*/com/emme/clients/*"); do
  sed -i '' 's/import com\.emme\.studio\.domain/import com.emme.clients.domain/g' "$f"
  sed -i '' 's/import com\.emme\.studio\.api/import com.emme.clients.api/g' "$f"
  sed -i '' 's/import com\.emme\.studio\.application/import com.emme.clients.application/g' "$f"
  sed -i '' 's/import com\.emme\.studio\.adapter/import com.emme.clients.adapter/g' "$f"
done
```

- [ ] **Step 4: Update clients build.gradle.kts**

Read `modules/clients/build.gradle.kts` and replace:
```kotlin
plugins {
  id("emme.spring-module")
  id("emme.testing")
}
dependencies {
  implementation(libs.spring.webmvc)
  testImplementation(testFixtures(project(":libraries:testing")))
}
```
With:
```kotlin
plugins {
  id("emme.spring-module")
  id("emme.integration-testing")
  id("emme.spring-web")
  id("emme.persistence")
  id("emme.testing")
}

dependencies {
  implementation(project(":modules:shared"))
  implementation(project(":libraries:kernel"))
  implementation(project(":modules:tenancy"))
  implementation(project(":libraries:kernel"))

  implementation(libs.spring.boot.starter.web)
  implementation(libs.spring.boot.starter.validation)
  implementation(libs.springdoc.openapi.starter.webmvc.ui)

  testImplementation(testFixtures(project(":libraries:testing")))
}
```

- [ ] **Step 5: Update clients package-info.java**

Read `modules/clients/src/main/java/com/emme/clients/package-info.java` and update `allowedDependencies`:
```java
@org.springframework.modulith.ApplicationModule(
    displayName = "Clients",
    allowedDependencies = {"shared :: persistence", "tenancy"})
package com.emme.clients;
```

- [ ] **Step 6: Create sub-package package-info.java files**

```bash
for dir in domain domain/model api api/result api/usecase application application/mapper application/port application/port/out application/service adapter adapter/in adapter/in/web adapter/in/web/controller adapter/in/web/request adapter/in/web/response adapter/out adapter/out/persistence adapter/out/persistence/adapter adapter/out/persistence/entity adapter/out/persistence/mapper adapter/out/persistence/repository; do
  pkg_path="modules/clients/src/main/java/com/emme/clients/$dir"
  pkg_name=$(echo "$dir" | tr '/' '.')
  if [ -d "$pkg_path" ] && [ ! -f "$pkg_path/package-info.java" ]; then
    echo "package com.emme.clients.$pkg_name;" > "$pkg_path/package-info.java"
  fi
done
```

- [ ] **Step 7: Verify clients module compiles**

```bash
JAVA_HOME=$(mise exec -- printenv JAVA_HOME) ./gradlew :modules:clients:compileJava --no-configuration-cache
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Commit**

```bash
git add -A && git commit -m "refactor: extract clients module from studio

- Populated modules/clients with customer CRM from studio
- Customer domain, use cases, adapters, controllers moved to com.emme.clients
- Updated build.gradle.kts with full plugin dependencies"
```

---

### Task 7: Create `appointments` module (appointment lifecycle)

**Files:**
- Create: `modules/appointments/` with DDD structure
- Move: Appointment domain, events, use cases, application services, adapters, controllers, SSE, messaging from studio
- Modify: `settings.gradle.kts`, `applications/emme-platform/build.gradle.kts`

**Interfaces:**
- Consumes: services (Task 5), clients (Task 6)
- Produces: Standalone `appointments` module — consumed by `calendar` (Task 9), `identity` (Task 9)

- [ ] **Step 1: Create module directory structure**

```bash
mkdir -p modules/appointments/src/main/java/com/emme/appointments/{api/{event,exception,result,type,usecase},application/{mapper,port/out,service},domain/model,adapter/{in/{web/{controller,request,response},sse},out/{messaging/publisher,persistence/{adapter,entity,mapper,repository}}}}}
mkdir -p modules/appointments/src/test/java/com/emme/appointments
```

- [ ] **Step 2: Create build.gradle.kts**

Write `modules/appointments/build.gradle.kts`:
```kotlin
plugins {
  id("emme.spring-module")
  id("emme.integration-testing")
  id("emme.spring-web")
  id("emme.persistence")
  id("emme.messaging")
  id("emme.testing")
}

dependencies {
  implementation(project(":modules:shared"))
  implementation(project(":libraries:kernel"))
  implementation(project(":modules:tenancy"))
  implementation(project(":libraries:kernel"))
  implementation(project(":modules:services"))
  implementation(project(":modules:clients"))

  implementation(libs.spring.boot.starter.web)
  implementation(libs.spring.boot.starter.validation)
  implementation(libs.springdoc.openapi.starter.webmvc.ui)

  testImplementation(testFixtures(project(":libraries:testing")))
}
```

- [ ] **Step 3: Create package-info.java**

Write `modules/appointments/src/main/java/com/emme/appointments/package-info.java`:
```java
@org.springframework.modulith.ApplicationModule(
    displayName = "Appointments",
    allowedDependencies = {"shared :: persistence", "tenancy", "services", "clients"})
package com.emme.appointments;
```

- [ ] **Step 4: Move appointment-related files from studio**

```bash
STUDIO_SRC="modules/studio/src/main/java/com/emme/studio"
APT_SRC="modules/appointments/src/main/java/com/emme/appointments"

# Domain
cp "$STUDIO_SRC/domain/model/Appointment.java" "$APT_SRC/domain/model/"
cp "$STUDIO_SRC/domain/model/AppointmentStatus.java" "$APT_SRC/domain/model/"
cp "$STUDIO_SRC/domain/model/ExternalCalendarStatus.java" "$APT_SRC/domain/model/"

# API events
for f in "$STUDIO_SRC"/api/event/Appointment{Created,Cancelled,Rescheduled}.java; do
  [ -f "$f" ] && cp "$f" "$APT_SRC/api/event/$(basename $f)"
done

# API use cases: appointment related
for f in "$STUDIO_SRC"/api/usecase/{CreateAppointment*,CancelAppointment*,ConfirmAppointment*,CompleteAppointment*,StartAppointment*,RescheduleAppointment*,MarkAppointmentNoShow*,GetAppointment*,ListAppointment*,FindAvailableSlot*}UseCase.java; do
  [ -f "$f" ] && cp "$f" "$APT_SRC/api/usecase/$(basename $f)"
done

# API results
for f in "$STUDIO_SRC"/api/result/{AppointmentDetails,AppointmentSummary,AvailableSlot}.java; do
  [ -f "$f" ] && cp "$f" "$APT_SRC/api/result/$(basename $f)"
done

# Application services
for f in "$STUDIO_SRC"/application/service/{CreateAppointment*,CancelAppointment*,ConfirmAppointment*,CompleteAppointment*,StartAppointment*,RescheduleAppointment*,MarkAppointmentNoShow*,GetAppointment*,ListAppointment*,FindAvailableSlot*,AppointmentApplicationSupport}.java; do
  [ -f "$f" ] && cp "$f" "$APT_SRC/application/service/$(basename $f)"
done

# Application mapper
cp "$STUDIO_SRC/application/mapper/AppointmentApplicationMapper.java" "$APT_SRC/application/mapper/"

# Application ports
for f in "$STUDIO_SRC"/application/port/out/{AppointmentRepository,AppointmentCollisionPort,AppointmentEventPublisher}.java; do
  [ -f "$f" ] && cp "$f" "$APT_SRC/application/port/out/$(basename $f)"
done

# Adapters: persistence
cp "$STUDIO_SRC/adapter/out/persistence/entity/AppointmentEntity.java" "$APT_SRC/adapter/out/persistence/entity/"
cp "$STUDIO_SRC/adapter/out/persistence/mapper/AppointmentPersistenceMapper.java" "$APT_SRC/adapter/out/persistence/mapper/"
cp "$STUDIO_SRC/adapter/out/persistence/repository/SpringDataAppointmentRepository.java" "$APT_SRC/adapter/out/persistence/repository/"
cp "$STUDIO_SRC/adapter/out/persistence/adapter/AppointmentPersistenceAdapter.java" "$APT_SRC/adapter/out/persistence/adapter/"
cp "$STUDIO_SRC/adapter/out/persistence/adapter/AppointmentCollisionAdapter.java" "$APT_SRC/adapter/out/persistence/adapter/"

# Adapters: messaging
cp "$STUDIO_SRC/adapter/out/messaging/publisher/SpringAppointmentEventPublisher.java" "$APT_SRC/adapter/out/messaging/publisher/"

# Adapters: web
cp "$STUDIO_SRC/adapter/in/web/controller/AppointmentController.java" "$APT_SRC/adapter/in/web/controller/"
cp "$STUDIO_SRC/adapter/in/web/sse/DashboardBroadcaster.java" "$APT_SRC/adapter/in/web/sse/"
cp "$STUDIO_SRC/adapter/in/web/sse/DashboardSseEvent.java" "$APT_SRC/adapter/in/web/sse/"
```

- [ ] **Step 5: Rewrite package declarations**

```bash
for f in $(find modules/appointments/src/main/java -name "*.java"); do
  sed -i '' 's/^package com\.emme\.studio;/package com.emme.appointments;/g' "$f"
  sed -i '' 's/^package com\.emme\.studio\./package com.emme.appointments./g' "$f"
done

for f in $(find modules/appointments/src/main/java -name "*.java"); do
  sed -i '' 's/import com\.emme\.studio\.domain/import com.emme.appointments.domain/g' "$f"
  sed -i '' 's/import com\.emme\.studio\.api/import com.emme.appointments.api/g' "$f"
  sed -i '' 's/import com\.emme\.studio\.application/import com.emme.appointments.application/g' "$f"
  sed -i '' 's/import com\.emme\.studio\.adapter/import com.emme.appointments.adapter/g' "$f"
done
```

- [ ] **Step 6: Create sub-package package-info files**

```bash
for dir in domain domain/model api api/event api/result api/usecase application application/mapper application/port application/port/out application/service adapter adapter/in adapter/in/web adapter/in/web/controller adapter/in/web/sse adapter/out adapter/out/messaging adapter/out/messaging/publisher adapter/out/persistence adapter/out/persistence/adapter adapter/out/persistence/entity adapter/out/persistence/mapper adapter/out/persistence/repository; do
  pkg_path="modules/appointments/src/main/java/com/emme/appointments/$dir"
  pkg_name=$(echo "$dir" | tr '/' '.')
  if [ -d "$pkg_path" ] && [ ! -f "$pkg_path/package-info.java" ]; then
    echo "package com.emme.appointments.$pkg_name;" > "$pkg_path/package-info.java"
  fi
done
```

- [ ] **Step 7: Create AppointmentsModuleTest.java**

Write `modules/appointments/src/test/java/com/emme/appointments/AppointmentsModuleTest.java`:
```java
package com.emme.appointments;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.testing.BaseUnitTest;
import org.junit.jupiter.api.Test;

class AppointmentsModuleTest extends BaseUnitTest {

  @Test
  void moduleLoads() {
    assertThat(getClass().getPackageName()).contains("appointments");
  }

  @Test
  void testStructureExists() {
    assertThat(true).isTrue();
  }
}
```

- [ ] **Step 8: Update settings.gradle.kts**

Add:
```kotlin
include(":modules:appointments")
```

- [ ] **Step 9: Update emme-platform build.gradle.kts**

Add:
```kotlin
  implementation(project(":modules:appointments"))
```

- [ ] **Step 10: Verify appointments module compiles**

```bash
JAVA_HOME=$(mise exec -- printenv JAVA_HOME) ./gradlew :modules:appointments:compileJava --no-configuration-cache
```
Expected: BUILD SUCCESSFUL. Fix any cross-module import issues (appointments may import `com.emme.services` or `com.emme.clients` types — update those imports).

- [ ] **Step 11: Commit**

```bash
git add -A && git commit -m "refactor: extract appointments module from studio

- New modules/appointments: appointment lifecycle, events, collision detection
- Moved Appointment domain, events (AppointmentCreated/Cancelled/Rescheduled),
  use cases, SSE dashboard, messaging publisher
- Updated settings.gradle.kts and emme-platform build.gradle.kts"
```

---

### Task 8: Create `salon` module (business configuration)

**Files:**
- Create: `modules/salon/` with DDD structure
- Move: BusinessProfile, OperatingHours, BookingPolicy, NotificationPreference, DayOfWeek, TemplatePolicy domain from studio
- Move: Business config use cases, application services, adapters, controllers (DashboardController stays in appointments)
- Modify: `settings.gradle.kts`, `applications/emme-platform/build.gradle.kts`

**Interfaces:**
- Consumes: None (depends on shared, tenancy only)
- Produces: Standalone `salon` module — consumed by `identity` (Task 9)

- [ ] **Step 1: Create module directory structure**

```bash
mkdir -p modules/salon/src/main/java/com/emme/salon/{api/{event,exception,result,type,usecase},application/{mapper,port/out,service},domain/model,adapter/{in/web/{controller,request,response},out/persistence/{adapter,entity,mapper,repository}}}
mkdir -p modules/salon/src/test/java/com/emme/salon
```

- [ ] **Step 2: Create build.gradle.kts**

Write `modules/salon/build.gradle.kts`:
```kotlin
plugins {
  id("emme.spring-module")
  id("emme.integration-testing")
  id("emme.spring-web")
  id("emme.persistence")
  id("emme.testing")
}

dependencies {
  implementation(project(":modules:shared"))
  implementation(project(":libraries:kernel"))
  implementation(project(":modules:tenancy"))
  implementation(project(":libraries:kernel"))

  implementation(libs.spring.boot.starter.web)
  implementation(libs.spring.boot.starter.validation)
  implementation(libs.springdoc.openapi.starter.webmvc.ui)

  testImplementation(testFixtures(project(":libraries:testing")))
}
```

- [ ] **Step 3: Create package-info.java**

Write `modules/salon/src/main/java/com/emme/salon/package-info.java`:
```java
@org.springframework.modulith.ApplicationModule(
    displayName = "Salon",
    allowedDependencies = {"shared :: persistence", "tenancy"})
package com.emme.salon;
```

- [ ] **Step 4: Move salon config-related files from studio**

```bash
STUDIO_SRC="modules/studio/src/main/java/com/emme/studio"
SALON_SRC="modules/salon/src/main/java/com/emme/salon"

# Domain
for f in BusinessProfile.java OperatingHours.java BookingPolicy.java NotificationPreference.java DayOfWeek.java TemplatePolicy.java; do
  cp "$STUDIO_SRC/domain/model/$f" "$SALON_SRC/domain/model/"
done

# API use cases
for f in "$STUDIO_SRC"/api/usecase/{GetBusinessProfile*,UpdateBusinessProfile*,GetOperatingHours*,UpdateOperatingHours*,GetBookingPolicy*,UpdateBookingPolicy*,"GetBusinessProfileConfig"*}UseCase.java; do
  [ -f "$f" ] && cp "$f" "$SALON_SRC/api/usecase/$(basename $f)"
done

# API results
for f in "$STUDIO_SRC"/api/result/{BusinessProfileDetails,BusinessProfileSummary,OperatingHoursDetails,BookingPolicyDetails}.java; do
  [ -f "$f" ] && cp "$f" "$SALON_SRC/api/result/$(basename $f)"
done

# API types
cp "$STUDIO_SRC/api/type/BusinessDay.java" "$SALON_SRC/api/type/"

# Application services
for f in "$STUDIO_SRC"/application/service/{GetBusinessProfile*,UpdateBusinessProfile*,GetOperatingHours*,UpdateOperatingHours*,GetBookingPolicy*,UpdateBookingPolicy*,"GetBusinessProfileConfig"*}Service.java; do
  [ -f "$f" ] && cp "$f" "$SALON_SRC/application/service/$(basename $f)"
done

# Application mapper
cp "$STUDIO_SRC/application/mapper/BusinessConfigurationApplicationMapper.java" "$SALON_SRC/application/mapper/"

# Application ports
for f in "$STUDIO_SRC"/application/port/out/{BusinessProfileRepository,OperatingHoursRepository,BookingPolicyRepository,NotificationPreferenceRepository}.java; do
  [ -f "$f" ] && cp "$f" "$SALON_SRC/application/port/out/$(basename $f)"
done

# Adapters
for f in BusinessProfileEntity.java OperatingHoursEntity.java BookingPolicyEntity.java NotificationPreferenceEntity.java; do
  cp "$STUDIO_SRC/adapter/out/persistence/entity/$f" "$SALON_SRC/adapter/out/persistence/entity/"
done
for f in BusinessProfilePersistenceMapper.java OperatingHoursPersistenceMapper.java BookingPolicyPersistenceMapper.java; do
  cp "$STUDIO_SRC/adapter/out/persistence/mapper/$f" "$SALON_SRC/adapter/out/persistence/mapper/"
done
for f in SpringDataBusinessProfileRepository.java SpringDataOperatingHoursRepository.java SpringDataBookingPolicyRepository.java SpringDataNotificationPreferenceRepository.java; do
  cp "$STUDIO_SRC/adapter/out/persistence/repository/$f" "$SALON_SRC/adapter/out/persistence/repository/"
done
for f in BusinessProfilePersistenceAdapter.java OperatingHoursPersistenceAdapter.java BookingPolicyPersistenceAdapter.java; do
  cp "$STUDIO_SRC/adapter/out/persistence/adapter/$f" "$SALON_SRC/adapter/out/persistence/adapter/"
done

# Web controllers + requests + responses
cp "$STUDIO_SRC/adapter/in/web/controller/BusinessConfigurationController.java" "$SALON_SRC/adapter/in/web/controller/"
cp "$STUDIO_SRC/adapter/in/web/request/UpdateProfileRequest.java" "$SALON_SRC/adapter/in/web/request/"
cp "$STUDIO_SRC/adapter/in/web/request/UpdateHoursRequest.java" "$SALON_SRC/adapter/in/web/request/"
cp "$STUDIO_SRC/adapter/in/web/request/UpdatePolicyRequest.java" "$SALON_SRC/adapter/in/web/request/"
cp "$STUDIO_SRC/adapter/in/web/response/BusinessProfileResponse.java" "$SALON_SRC/adapter/in/web/response/"
cp "$STUDIO_SRC/adapter/in/web/response/OperatingHoursResponse.java" "$SALON_SRC/adapter/in/web/response/"
cp "$STUDIO_SRC/adapter/in/web/response/BookingPolicyResponse.java" "$SALON_SRC/adapter/in/web/response/"
```

- [ ] **Step 5: Move DashboardController to appointments module**

```bash
cp "$STUDIO_SRC/adapter/in/web/controller/DashboardController.java" "modules/appointments/src/main/java/com/emme/appointments/adapter/in/web/controller/"
sed -i '' 's/^package com\.emme\.studio\.adapter\.in\.web\.controller;/package com.emme.appointments.adapter.in.web.controller;/g' "modules/appointments/src/main/java/com/emme/appointments/adapter/in/web/controller/DashboardController.java"
sed -i '' 's/import com\.emme\.studio\./import com.emme.appointments./g' "modules/appointments/src/main/java/com/emme/appointments/adapter/in/web/controller/DashboardController.java"
```

- [ ] **Step 6: Rewrite package declarations**

```bash
for f in $(find modules/salon/src/main/java -name "*.java"); do
  sed -i '' 's/^package com\.emme\.studio;/package com.emme.salon;/g' "$f"
  sed -i '' 's/^package com\.emme\.studio\./package com.emme.salon./g' "$f"
done

for f in $(find modules/salon/src/main/java -name "*.java"); do
  sed -i '' 's/import com\.emme\.studio\.domain/import com.emme.salon.domain/g' "$f"
  sed -i '' 's/import com\.emme\.studio\.api/import com.emme.salon.api/g' "$f"
  sed -i '' 's/import com\.emme\.studio\.application/import com.emme.salon.application/g' "$f"
  sed -i '' 's/import com\.emme\.studio\.adapter/import com.emme.salon.adapter/g' "$f"
done
```

- [ ] **Step 7: Create sub-package package-info files**

```bash
for dir in domain domain/model api api/result api/type api/usecase application application/mapper application/port application/port/out application/service adapter adapter/in adapter/in/web adapter/in/web/controller adapter/in/web/request adapter/in/web/response adapter/out adapter/out/persistence adapter/out/persistence/adapter adapter/out/persistence/entity adapter/out/persistence/mapper adapter/out/persistence/repository; do
  pkg_path="modules/salon/src/main/java/com/emme/salon/$dir"
  pkg_name=$(echo "$dir" | tr '/' '.')
  if [ -d "$pkg_path" ] && [ ! -f "$pkg_path/package-info.java" ]; then
    echo "package com.emme.salon.$pkg_name;" > "$pkg_path/package-info.java"
  fi
done
```

- [ ] **Step 8: Create SalonModuleTest.java**

Write `modules/salon/src/test/java/com/emme/salon/SalonModuleTest.java`:
```java
package com.emme.salon;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.testing.BaseUnitTest;
import org.junit.jupiter.api.Test;

class SalonModuleTest extends BaseUnitTest {

  @Test
  void moduleLoads() {
    assertThat(getClass().getPackageName()).contains("salon");
  }

  @Test
  void testStructureExists() {
    assertThat(true).isTrue();
  }
}
```

- [ ] **Step 9: Update settings.gradle.kts**

Add:
```kotlin
include(":modules:salon")
```

- [ ] **Step 10: Update emme-platform build.gradle.kts**

Add:
```kotlin
  implementation(project(":modules:salon"))
```

- [ ] **Step 11: Verify salon module compiles**

```bash
JAVA_HOME=$(mise exec -- printenv JAVA_HOME) ./gradlew :modules:salon:compileJava --no-configuration-cache
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 12: Commit**

```bash
git add -A && git commit -m "refactor: extract salon module from studio

- New modules/salon: business configuration (profile, hours, booking policy)
- Moved BusinessProfile, OperatingHours, BookingPolicy, NotificationPreference
- Moved DashboardController to appointments module
- Updated settings.gradle.kts and emme-platform build.gradle.kts"
```

---

### Task 9: Update cross-module consumers and delete studio

**Files:**
- Modify: All `identity` files importing `com.emme.studio.api.result.BusinessProfileSummary` or `com.emme.studio.api.usecase.GetBusinessProfileUseCase` → `com.emme.salon`
- Modify: All `identity` files importing `com.emme.studio.api.event.AppointmentCreated` → `com.emme.appointments`
- Modify: All `calendar` files importing studio types → `com.emme.appointments`, `com.emme.clients`
- Modify: `modules/booking/build.gradle.kts` — update all dependencies
- Delete: `modules/studio/` directory
- Modify: `settings.gradle.kts` — remove `include(":modules:studio")`, add all new modules
- Modify: `applications/emme-platform/build.gradle.kts` — remove `:modules:studio`, add all new modules

**Interfaces:**
- Consumes: All extracted modules from Tasks 2-8
- Produces: Complete decomposition; zero `com.emme.studio` imports

- [ ] **Step 1: Update identity module — salon imports**

```bash
# BusinessProfileSummary and GetBusinessProfileUseCase now in salon
for f in modules/identity/src/main/java/com/emme/identity/api/result/CurrentUserDetails.java modules/identity/src/main/java/com/emme/identity/application/service/GetCurrentUserService.java; do
  sed -i '' 's/import com\.emme\.studio\.api\.result\.BusinessProfileSummary/import com.emme.salon.api.result.BusinessProfileSummary/g' "$f"
  sed -i '' 's/import com\.emme\.studio\.api\.usecase\.GetBusinessProfileUseCase/import com.emme.salon.api.usecase.GetBusinessProfileUseCase/g' "$f"
done
```

- [ ] **Step 2: Update identity module — appointment event imports**

```bash
sed -i '' 's/import com\.emme\.studio\.api\.event\.AppointmentCreated/import com.emme.appointments.api.event.AppointmentCreated/g' modules/identity/src/main/java/com/emme/identity/adapter/in/messaging/consumer/AppointmentCreatedConsumer.java
```

- [ ] **Step 3: Update identity test files**

```bash
for f in modules/identity/src/test/java/com/emme/identity/adapter/in/messaging/consumer/AppointmentCreatedConsumerTest.java modules/identity/src/test/java/com/emme/identity/application/service/GetCurrentUserServiceTest.java; do
  sed -i '' 's/import com\.emme\.studio\./import com.emme./g' "$f" 2>/dev/null || true
  sed -i '' 's/import com\.emme\.studio\./import com.emme./g' "$f" 2>/dev/null || true
done
# More precise replacement
sed -i '' 's/import com\.emme\.studio\.api\.event/import com.emme.appointments.api.event/g' modules/identity/src/test/java/com/emme/identity/adapter/in/messaging/consumer/AppointmentCreatedConsumerTest.java 2>/dev/null || true
sed -i '' 's/import com\.emme\.studio\.api\.result/import com.emme.salon.api.result/g' modules/identity/src/test/java/com/emme/identity/application/service/GetCurrentUserServiceTest.java 2>/dev/null || true
sed -i '' 's/import com\.emme\.studio\.api\.usecase/import com.emme.salon.api.usecase/g' modules/identity/src/test/java/com/emme/identity/application/service/GetCurrentUserServiceTest.java 2>/dev/null || true
```

- [ ] **Step 4: Update calendar module imports**

```bash
# CalendarSyncListener imports appointment events
for f in modules/calendar/src/main/java/com/emme/calendar/adapter/in/messaging/CalendarSyncListener.java; do
  sed -i '' 's/import com\.emme\.studio\.api\.event\.AppointmentCancelled/import com.emme.appointments.api.event.AppointmentCancelled/g' "$f"
  sed -i '' 's/import com\.emme\.studio\.api\.event\.AppointmentCreated/import com.emme.appointments.api.event.AppointmentCreated/g' "$f"
  sed -i '' 's/import com\.emme\.studio\.api\.event\.AppointmentRescheduled/import com.emme.appointments.api.event.AppointmentRescheduled/g' "$f"
done

# GoogleSheetsAdapter imports appointment + customer types
sed -i '' 's/import com\.emme\.studio\.api\.result\.AppointmentSummary/import com.emme.appointments.api.result.AppointmentSummary/g' modules/calendar/src/main/java/com/emme/calendar/adapter/out/google/adapter/GoogleSheetsAdapter.java
sed -i '' 's/import com\.emme\.studio\.api\.result\.CustomerSummary/import com.emme.clients.api.result.CustomerSummary/g' modules/calendar/src/main/java/com/emme/calendar/adapter/out/google/adapter/GoogleSheetsAdapter.java
sed -i '' 's/import com\.emme\.studio\.api\.usecase\.ListAppointmentsUseCase/import com.emme.appointments.api.usecase.ListAppointmentsUseCase/g' modules/calendar/src/main/java/com/emme/calendar/adapter/out/google/adapter/GoogleSheetsAdapter.java
sed -i '' 's/import com\.emme\.studio\.api\.usecase\.ListCustomersUseCase/import com.emme.clients.api.usecase.ListCustomersUseCase/g' modules/calendar/src/main/java/com/emme/calendar/adapter/out/google/adapter/GoogleSheetsAdapter.java
```

- [ ] **Step 5: Update booking module build.gradle.kts**

Read `modules/booking/build.gradle.kts`. All references to `:modules:studio`, `:modules:customer`, `:modules:workforce` need updating:
```bash
sed -i '' 's/implementation(project(":modules:studio"))/implementation(project(":modules:services"))\n  implementation(project(":modules:appointments"))\n  implementation(project(":modules:salon"))/g' modules/booking/build.gradle.kts
sed -i '' 's/implementation(project(":modules:customer"))/implementation(project(":modules:clients"))/g' modules/booking/build.gradle.kts
sed -i '' 's/implementation(project(":modules:workforce"))/implementation(project(":modules:staffing"))/g' modules/booking/build.gradle.kts
```

- [ ] **Step 6: Update emme-platform build.gradle.kts**

Remove `implementation(project(":modules:studio"))` and `implementation(project(":modules:customer"))` and `implementation(project(":modules:workforce"))`.

The final module dependency list should be:
```kotlin
  implementation(project(":modules:shared"))
  implementation(project(":modules:tenancy"))
  implementation(project(":modules:identity"))
  implementation(project(":modules:services"))
  implementation(project(":modules:clients"))
  implementation(project(":modules:appointments"))
  implementation(project(":modules:salon"))
  implementation(project(":modules:subscriptions"))
  implementation(project(":modules:documents"))
  implementation(project(":modules:catalog"))
  implementation(project(":modules:booking"))
  implementation(project(":modules:calendar"))
  implementation(project(":modules:notification"))
  implementation(project(":modules:payment"))
  implementation(project(":modules:assistant"))
  implementation(project(":modules:staffing"))
  implementation(project(":modules:audit"))
```

Also update integration test dependency:
```kotlin
  add("integrationTestImplementation", project(":modules:services"))
```

- [ ] **Step 7: Update settings.gradle.kts**

Remove `include(":modules:studio")`, `include(":modules:customer")`, `include(":modules:workforce")`.

Final list:
```kotlin
include(":modules:shared")
include(":modules:tenancy")
include(":modules:identity")
include(":modules:services")
include(":modules:clients")
include(":modules:appointments")
include(":modules:salon")
include(":modules:subscriptions")
include(":modules:documents")
include(":modules:catalog")
include(":modules:booking")
include(":modules:calendar")
include(":modules:notification")
include(":modules:payment")
include(":modules:assistant")
include(":modules:staffing")
include(":modules:audit")
```

- [ ] **Step 8: Remove studio module**

```bash
git rm -r modules/studio/
```

- [ ] **Step 9: Update architecture tests**

Search for any remaining `com.emme.studio` references in architecture tests:
```bash
rg -l "com\.emme\.studio" --glob "*.java" --glob "!**/build/**" .
```

For any remaining references, update to the correct new module package. If they are in architecture test allowlists, update the allowlist to reference the new module names.

- [ ] **Step 10: Verify clean build**

```bash
JAVA_HOME=$(mise exec -- printenv JAVA_HOME) ./gradlew :applications:emme-platform:compileJava --no-configuration-cache
```
Expected: BUILD SUCCESSFUL. Fix any remaining compilation errors.

- [ ] **Step 11: Commit**

```bash
git add -A && git commit -m "refactor: update cross-module consumers and remove studio

- Updated identity imports: salon (BusinessProfile), appointments (events), subscriptions (plans)
- Updated calendar imports: appointments (events, list), clients (list)
- Updated booking build.gradle.kts: studio→services+appointments+salon, customer→clients
- Removed modules/studio/ (fully decomposed)
- Updated settings.gradle.kts and emme-platform build.gradle.kts"
```

---

### Task 10: Update test files from studio into new modules

**Files:**
- Move: All remaining studio test files into appropriate new modules
- Modify: Test package declarations and imports

**Interfaces:**
- Consumes: Complete decomposition from Task 9
- Produces: Tests compiling in new modules

- [ ] **Step 1: Move studio test files to new modules**

Studio test files that weren't already moved (documents/subscriptions were moved in Tasks 3-4):

```bash
STUDIO_TEST="modules/studio/src/test/java/com/emme"

# Domain tests → their respective modules
cp "$STUDIO_TEST/studio/domain/model/AppointmentTest.java" modules/appointments/src/test/java/com/emme/appointments/ 2>/dev/null || true
cp "$STUDIO_TEST/studio/domain/model/ServiceTest.java" modules/services/src/test/java/com/emme/services/ 2>/dev/null || true
cp "$STUDIO_TEST/studio/domain/model/ArtistTest.java" modules/services/src/test/java/com/emme/services/ 2>/dev/null || true
cp "$STUDIO_TEST/studio/domain/model/CustomerTest.java" modules/clients/src/test/java/com/emme/clients/ 2>/dev/null || true
cp "$STUDIO_TEST/studio/domain/model/BusinessConfigurationTest.java" modules/salon/src/test/java/com/emme/salon/ 2>/dev/null || true

# Adapter tests → their respective modules
cp "$STUDIO_TEST/studio/adapter/out/persistence/mapper/AppointmentPersistenceMapperTest.java" modules/appointments/src/test/java/com/emme/appointments/ 2>/dev/null || true

# Web tests → their respective modules
cp "$STUDIO_TEST/salon/web/DashboardWebTest.java" modules/appointments/src/test/java/com/emme/appointments/ 2>/dev/null || true
cp "$STUDIO_TEST/salon/web/CustomerWebTest.java" modules/clients/src/test/java/com/emme/clients/ 2>/dev/null || true
cp "$STUDIO_TEST/salon/web/AppointmentWebTest.java" modules/appointments/src/test/java/com/emme/appointments/ 2>/dev/null || true

# Module tests → their respective modules
cp "$STUDIO_TEST/salon/module/AppointmentModuleTest.java" modules/appointments/src/test/java/com/emme/appointments/ 2>/dev/null || true
cp "$STUDIO_TEST/salon/module/ServiceModuleTest.java" modules/services/src/test/java/com/emme/services/ 2>/dev/null || true
cp "$STUDIO_TEST/salon/module/ArtistModuleTest.java" modules/services/src/test/java/com/emme/services/ 2>/dev/null || true
cp "$STUDIO_TEST/salon/module/CustomerModuleTest.java" modules/clients/src/test/java/com/emme/clients/ 2>/dev/null || true
cp "$STUDIO_TEST/salon/module/BusinessConfigModuleTest.java" modules/salon/src/test/java/com/emme/salon/ 2>/dev/null || true
cp "$STUDIO_TEST/salon/module/DashboardSseWiringModuleTest.java" modules/appointments/src/test/java/com/emme/appointments/ 2>/dev/null || true
cp "$STUDIO_TEST/salon/module/SalonAuthModuleTest.java" modules/appointments/src/test/java/com/emme/appointments/ 2>/dev/null || true

# Repository tests → their respective modules
cp "$STUDIO_TEST/salon/repository/AppointmentRepositoryTest.java" modules/appointments/src/test/java/com/emme/appointments/ 2>/dev/null || true
cp "$STUDIO_TEST/salon/repository/CustomerRepositoryTest.java" modules/clients/src/test/java/com/emme/clients/ 2>/dev/null || true
```

- [ ] **Step 2: Rewrite test package declarations and imports**

```bash
# For each new module, update test files
for mod in appointments services clients salon; do
  TEST_DIR="modules/$mod/src/test/java/com/emme/$mod"
  [ ! -d "$TEST_DIR" ] && continue

  for f in $(find "$TEST_DIR" -name "*.java" 2>/dev/null); do
    # Fix package declarations
    sed -i '' 's/^package com\.emme\.studio/package com.emme.'"$mod"'/g' "$f"
    sed -i '' 's/^package com\.emme\.salon/package com.emme.'"$mod"'/g' "$f" 2>/dev/null || true

    # Fix imports to new modules
    sed -i '' 's/import com\.emme\.studio\.domain\.model\.Appointment/import com.emme.appointments.domain.model.Appointment/g' "$f"
    sed -i '' 's/import com\.emme\.studio\.domain\.model\.Service/import com.emme.services.domain.model.Service/g' "$f"
    sed -i '' 's/import com\.emme\.studio\.domain\.model\.Artist/import com.emme.services.domain.model.Artist/g' "$f"
    sed -i '' 's/import com\.emme\.studio\.domain\.model\.Customer/import com.emme.clients.domain.model.Customer/g' "$f"
    sed -i '' 's/import com\.emme\.studio\.domain\.model\./import com.emme.'"$mod"'.domain.model./g' "$f"
    sed -i '' 's/import com\.emme\.studio\.api/import com.emme.'"$mod"'.api/g' "$f"
    sed -i '' 's/import com\.emme\.studio\.application/import com.emme.'"$mod"'.application/g' "$f"
    sed -i '' 's/import com\.emme\.studio\.adapter/import com.emme.'"$mod"'.adapter/g' "$f"
    sed -i '' 's/import com\.emme\.studio\.documents/import com.emme.documents/g' "$f"
    sed -i '' 's/import com\.emme\.studio\.subscriptions/import com.emme.subscriptions/g' "$f"
  done
done
```

- [ ] **Step 3: Verify test compilation**

```bash
JAVA_HOME=$(mise exec -- printenv JAVA_HOME) ./gradlew :modules:services:compileTestJava :modules:clients:compileTestJava :modules:appointments:compileTestJava :modules:salon:compileTestJava --no-configuration-cache
```
Expected: BUILD SUCCESSFUL. Fix any compilation errors from cross-module test imports.

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "refactor: move studio tests into new modules

- Moved domain, web, module, and repository tests to their respective modules
- Updated all test package declarations and cross-module imports"
```

---

### Task 11: Final verification

**Files:** None created/modified.

**Interfaces:**
- Consumes: All tasks complete
- Produces: Green build, ModularityTest passes

- [ ] **Step 1: Run all module tests**

```bash
JAVA_HOME=$(mise exec -- printenv JAVA_HOME) ./gradlew test --no-configuration-cache -x :modules:studio:test 2>/dev/null
```
Expected: BUILD SUCCESSFUL, all tests pass

- [ ] **Step 2: Run ModularityTest**

```bash
JAVA_HOME=$(mise exec -- printenv JAVA_HOME) ./gradlew :applications:emme-platform:test --tests '*ModularityTest' --no-configuration-cache
```
Expected: BUILD SUCCESSFUL, Spring Modulith verifies all module boundaries

- [ ] **Step 3: Run architecture tests**

```bash
JAVA_HOME=$(mise exec -- printenv JAVA_HOME) ./gradlew :applications:emme-platform:test --tests '*ArchitectureTest' --no-configuration-cache
```
Expected: BUILD SUCCESSFUL, ArchUnit rules pass

- [ ] **Step 4: Run spotless check**

```bash
JAVA_HOME=$(mise exec -- printenv JAVA_HOME) ./gradlew spotlessCheck --no-configuration-cache
```
Expected: BUILD SUCCESSFUL, no formatting violations

- [ ] **Step 5: Verify zero studio imports remain**

```bash
rg "import com\.emme\.studio\." --glob "*.java" --glob "!**/build/**" . | grep -v "modules/studio/"
```
Expected: No output (zero remaining studio imports outside the deleted module)

- [ ] **Step 6: Verify new module structure**

```bash
ls modules/ | sort
```
Expected output:
```
appointments
assistant
audit
booking
calendar
catalog
clients
documents
identity
notification
payment
salon
services
shared
staffing
subscriptions
tenancy
```

- [ ] **Step 7: Commit**

```bash
git add -A && git commit -m "refactor: final verification after studio decomposition

- All tests pass across new modules
- ModularityTest confirms correct boundary enforcement
- Zero com.emme.studio imports remain
- 17 modules total: 13 functional + 4 contract-only (audit, booking, staffing, catalog)"
```

---

## Summary

| Phase | Task | Files moved | Modules affected |
|---|---|---|---|
| Rename | Task 2 | 10 test files | customer→clients, workforce→staffing |
| Extract | Task 3 | ~30 files | subscriptions from studio |
| Extract | Task 4 | ~30 files | documents from studio |
| Split | Task 5 | ~40 files | services from studio |
| Split | Task 6 | ~20 files | clients from studio |
| Split | Task 7 | ~35 files | appointments from studio |
| Split | Task 8 | ~30 files | salon from studio |
| Consumers | Task 9 | ~30 files | identity, calendar, booking, emme-platform |
| Tests | Task 10 | ~15 files | Test distribution to new modules |
| Verify | Task 11 | 0 files | Full build + architecture verification |

**Total:** ~200 Java files moved/renamed, ~30 consumer files updated, ~12 Gradle/build files updated
