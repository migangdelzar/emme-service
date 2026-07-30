# Emme Nails — Architecture & Code Flow

## System Architecture

```mermaid
graph TB
    subgraph "Infrastructure"
        PG[(PostgreSQL 18<br/>pgvector)]
        Redis[(Redis 7)]
        KC[Keycloak 26<br/>port :18080]
    end

    subgraph "Backend :8080"
        API[REST API<br/>controllers]
        AUTH[AuthController<br/>/api/auth/login]
        ID[Identity Module<br/>membership/roles]
        TEN[Tenancy Module<br/>routing/filter]
        STUDIO[Studio Module<br/>customers/services]
        SEED[DataSeeder<br/>demo data]
    end

    subgraph "Frontend :3000"
        VITE[Vite Dev Server]
        REACT[React SPA]
        PWA[PWA Manifest]
    end

    subgraph "E2E Tests"
        REST[REST E2E<br/>OkHttp + JUnit5]
        UI[UI E2E<br/>Playwright + Bun]
    end

    subgraph "Libraries"
        FN[functional]
        KNL[kernel]
        TST[testing]
        TC[test-containers]
    end

    API --> PG
    API --> Redis
    AUTH --> KC
    REST --> API
    UI --> VITE
    UI --> API
    STUDIO --> PG
    TEN --> PG
    SEED --> PG
```

---

## Auth Flow

```mermaid
sequenceDiagram
    participant Test as E2E Test
    participant Ext as E2eUserExtension
    participant Pool as E2eUserPool
    participant Backend as Backend :8080
    participant KC as Keycloak :18080
    participant DB as PostgreSQL

    Note over Test,DB: Pool Init (class-load)

    Pool->>Backend: POST /api/auth/login<br/>{email, password} x 10 users
    Backend->>KC: KeycloakAuthService password grant
    KC-->>Backend: accessToken + idToken + refreshToken
    Backend->>KC: GET /userinfo
    KC-->>Backend: user claims (sub, email, tenant_id)
    Backend-->>Pool: TokenLoginResponse {accessToken}
    Pool->>Pool: store TestUser(token, role, tenantId)

    Note over Test,DB: Test Execution

    Test->>Ext: @BeforeEach
    Ext->>Pool: acquire(role, tenantId)
    Pool-->>Ext: TestUser with token
    Ext->>Ext: create UserSession(baseUrl, testUser)
    Ext->>Test: inject UserSession parameter

    Test->>Backend: GET /api/v1/customers<br/>Authorization: Bearer {token}
    Backend->>Backend: BearerTokenAuthenticationFilter
    Backend->>KC: GET JWK certs (validate JWT)
    KC-->>Backend: public key
    Backend->>Backend: TenantContextFilter<br/>read tenant_id from JWT
    Backend->>DB: SET search_path TO demo_tenant
    Backend->>DB: SELECT * FROM customer
    DB-->>Backend: [Elena Garcia, Maria Lopez, ...]
    Backend-->>Test: HTTP 200 + JSON

    Test->>Ext: @AfterEach
    Ext->>Pool: release(userId)
```

---

## Multi-Tenant Schema Routing

```mermaid
sequenceDiagram
    participant HTTP as HTTP Request
    participant Filter as TenantContextFilter
    participant Aspect as TenantContextAspect
    participant DS as TenantRoutingDataSource
    participant PG as PostgreSQL

    HTTP->>Filter: GET /api/v1/customers<br/>Bearer {token}
    Filter->>Filter: extract tenant_id from JWT<br/>(or ?tenant= param, or hostname)
    Filter->>Filter: TenantContext.setCurrentTenant(id)

    Filter->>Aspect: @Around repository method
    Aspect->>PG: SELECT schema_name FROM tenant_registry
    PG-->>Aspect: demo_tenant
    Aspect->>PG: SET LOCAL search_path TO demo_tenant
    Aspect->>Aspect: delegate to original method

    Note over PG: All subsequent queries go to demo_tenant.*

    Filter->>DS: AbstractRoutingDataSource
    DS->>DS: DatabasePoolManager.getDataSource(databaseId)
    Note over DS: Routes to correct HikariCP connection pool
```

---

## E2E Extension Lifecycle

```mermaid
stateDiagram-v2
    [*] --> PoolInit: Class load
    PoolInit --> Available: 10 users in queue

    state "PER_METHOD (default)" as PM {
        [*] --> BeforeTest
        BeforeTest --> AcquireUser: acquire(role, tenant)
        AcquireUser --> InjectSession: create UserSession
        InjectSession --> RunTest: @Test executes
        RunTest --> ReleaseUser: @AfterEach
        ReleaseUser --> [*]: back to pool
    }

    state "PER_CLASS" as PC {
        [*] --> BeforeClass: @BeforeAll
        BeforeClass --> AcquireClass: acquire(role, tenant)
        AcquireClass --> RunTests: all @Test methods
        RunTests --> ReleaseClass: @AfterAll
        ReleaseClass --> [*]: back to pool
    }

    Available --> PM
    Available --> PC
```

---

## E2E Test Patterns

```mermaid
graph LR
    subgraph "Single User (common)"
        A1["@WithUser<br/>role=BUSINESS_OWNER"]
        A1 --> B1["void test<br/>(UserSession api)"]
    end

    subgraph "Method Override"
        A2["@WithUser<br/>role=FRONT_DESK<br/>on method"]
        A2 --> B2["void test<br/>(UserSession api)"]
    end

    subgraph "Per-Class (read-only)"
        A3["@WithUser<br/>lifecycle=PER_CLASS"]
        A3 --> B3["3 tests share<br/>same session"]
    end

    subgraph "Multi-User (manual)"
        A4["@WithUser<br/>role=OWNER<br/>Primary"]
        A4 --> B4["pool = E2eUserPool.INSTANCE"]
        B4 --> C4["other = pool.acquire<br/>(FRONT_DESK, studio-a)"]
        C4 --> D4["pool.release<br/>(other.userId)"]
    end
```

---

## E2E User Pool

```mermaid
sequenceDiagram
    participant T1 as Test 1
    participant T2 as Test 2
    participant Pool as E2eUserPool
    participant Q as Queue

    Pool->>Pool: init: login 10 users via /api/auth/login
    Pool->>Q: enqueue 10 TestUser {token, role, tenantId}

    T1->>Pool: acquire(BUSINESS_OWNER, "demo-salon")
    Pool->>Q: find matching
    Q-->>Pool: owner@demo-salon
    Pool-->>T1: UserSession

    T2->>Pool: acquire(FRONT_DESK, "demo-salon")
    Pool->>Q: find matching
    Q-->>Pool: front-desk@demo-salon
    Pool-->>T2: UserSession

    T1->>Pool: release(owner)
    Pool->>Pool: re-login fresh token
    Pool->>Q: returned

    T2->>Pool: release(desk)
    Pool->>Q: returned
```

---

## Library Dependency Graph

```mermaid
graph TB
    subgraph "libraries/"
        FUNC["functional<br/>URunnable, USupplier<br/>7 files · JDK only"]
        KERNEL["kernel<br/>TenantContext, CorrelationId<br/>ChannelType<br/>6 files"]
        TESTING["testing<br/>BaseSpringModuleTest<br/>H2 base classes"]
        TCONTAINERS["test-containers<br/>@PostgresIntegrationTest<br/>PG/Redis containers"]
    end

    subgraph "modules/"
        SHARED["shared<br/>BaseEntity, IdGenerator<br/>HybridSearch"]
        TENANCY["tenancy<br/>TenantProvisioning<br/>DataSource routing"]
        IDENTITY["identity<br/>Auth, FeatureFlags<br/>CurrentUser"]
        STUDIO["studio<br/>Customers, Services<br/>Appointments"]
        OTHERS["booking, catalog<br/>calendar, payment<br/>notification, assistant"]
    end

    subgraph "apps/"
        STUDIO_API["studio-api<br/>Assembled App"]
    end

    FUNC --> KERNEL
    FUNC --> SHARED
    KERNEL --> TENANCY
    SHARED --> TENANCY
    SHARED --> STUDIO
    STUDIO_API --> TENANCY
    STUDIO_API --> IDENTITY
    STUDIO_API --> STUDIO
    STUDIO_API --> OTHERS
    TENANCY --> STUDIO
```

---

## Database Schema Layout

```mermaid
graph TB
    subgraph "emme_core (system tables)"
        TR[tenant_registry<br/>slug, schema_name, status]
        DR[database_registry<br/>host, port, pool config]
        ROL[role · permission<br/>role_permission]
        MEM[membership<br/>user_id → tenant + role]
        TEN[tenant]
        FF[feature_flag · audit]
    end

    subgraph "demo_tenant (business data)"
        CUST[customer]
        SVC[service]
        ART[artist]
        APPT[appointment]
        SUB[subscription]
        DOC[document]
        NOTIF[notification]
        PAY[payment]
        CAL[calendar · google_oauth]
        CAT[catalog_item]
        CONV[conversation]
    end

    subgraph "public (shared)"
        EVENT[event_publication<br/>Spring Modulith]
    end
```

---

## REST vs UI E2E — Same Pattern, Different Framework

| Layer | REST E2E (Java) | UI E2E (TypeScript) |
|-------|----------------|---------------------|
| **Extension** | `E2eUserExtension` + `@BeforeEach` | `testWithUser.ts` custom Playwright fixture |
| **Annotation** | `@WithUser(role, tenant)` | `test({ metadata: { mode: 'mock' } })` |
| **Injection** | `UserSession` parameter | `{ authenticatedPage }` fixture |
| **Auth** | `POST /api/auth/login` → Bearer token | `localStorage` token injection (mock) or Keycloak OAuth2 (real) |
| **Multi-user** | `pool.acquire(role, tenant)` | `acquireUser()` from `userPool.ts` |
| **Provider** | — | `MockProvider` (page.route) / `RealProvider` (OAuth2) |

### UI E2E Architecture

```mermaid
graph TB
    subgraph "E2E Tests"
        subgraph "REST (Java)"
            EXT["E2eUserExtension<br/>@BeforeEach/@AfterEach"]
            ANNOT["@WithUser<br/>role=OWNER, tenant=demo-salon"]
            POOL["E2eUserPool<br/>10 users · backend login"]
            INJECT["UserSession parameter<br/>Bearer token → HTTP"]
        end

        subgraph "UI (TypeScript)"
            FIXTURE["testWithUser.ts<br/>custom Playwright fixture"]
            MOCK["MockProvider<br/>page.route() + localStorage"]
            REAL["RealProvider<br/>Keycloak OAuth2 · seed() no-op"]
            PAGE["{ authenticatedPage }<br/>injected fixture → browser"]
        end
    end

    subgraph "Backend :8080"
        AUTH["/api/auth/login"]
        API["REST endpoints"]
    end

    subgraph "Keycloak :18080"
        KC["OAuth2 /userinfo /certs"]
    end

    REST --> AUTH
    REST --> API
    AUTH --> KC
    UI --> API
    REAL --> KC
```

### UI E2E Fixture (TypeScript)

```typescript
// testWithUser.ts — Playwright custom fixture (mirrors E2eUserExtension)
import { test as base } from '@playwright/test';

const MODE = process.env.E2E_MODE || 'mock';

export const test = base.extend({
    authenticatedPage: async ({ page, testUser }, use) => {
        if (MODE === 'mock') {
            await new MockProvider().setup(page, testUser);
            await use(page);
        } else {
            await new RealProvider().setup(page);    // Keycloak OAuth2
            await use(page);
        }
    },
    testUser: [async ({}, use) => {
        const user = acquireUser();       // ← UserPool pattern
        await use(user);
        releaseUser(user.userId);
    }, { scope: 'test' }],
});

export { expect } from '@playwright/test';
```

```typescript
// customers.spec.ts — test usage
import { test, expect } from '@fixtures/testWithUser';

test.describe('Customers', () => {
    test.beforeEach(async ({ authenticatedPage, provider }) => {
        await provider.seed({ customers: mockCustomers });
        const clients = new ClientsPage(authenticatedPage);
        await clients.goto();
    });

    test('heading renders', async ({ authenticatedPage }) => {
        await expect(authenticatedPage.getByText('Clientes')).toBeVisible();
    });
});
```

---

## E2E Test Matrix

| Suite | Mode | Provider | Runner | Passed | Failed |
|-------|------|----------|--------|--------|--------|
| **REST E2E** | — | Backend login | JUnit 5 | 9/44 | 35 |
| **UI Mock** | mock | page.route() interception | Playwright | 23/48 | 8 |
| **UI Real** | real | Keycloak OAuth2 | Playwright | 8/48 | 40 |
| **Studio-api** | — | H2 + ArchUnit | JUnit 5 | 12/12 | 0 |
| **Integration** | — | Testcontainers PG | JUnit 5 | compile ✅ | needs Docker |

---

## Dev Quick Start

```bash
# 1. Infrastructure
docker compose up -d
docker-compose exec postgres psql -U emme -d emme \
  -c "CREATE EXTENSION IF NOT EXISTS vector"

# 2. Backend (DevTools hot reload)
SPRING_PROFILES_ACTIVE=local \
  ./gradlew :applications:studio-api:bootRun

# 3. Frontend (Vite HMR)
cd apps/emme-salon-app && bun dev

# 4. REST E2E
EMME_E2E_BASE_URL=http://localhost:8080 \
  ./gradlew :applications:studio-api:e2eTest

# 5. UI E2E Mock
cd e2e/src && bun run test

# 6. Studio-api tests
./gradlew :applications:studio-api:test

# 7. Integration tests (needs Docker)
./gradlew integrationTest
```
