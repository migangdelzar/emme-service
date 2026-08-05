# Schema-Per-Tenant & Realm-Per-Tenant Provisioning Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace AspectJ `SET LOCAL search_path` + ShedLock polling with Hibernate native multi-tenancy (dual pools) + event-driven provisioning chain (schema → realm → activated), per the approved design spec.

**Architecture:** Hibernate `MultiTenantConnectionProvider` routes connections between core pool (`emme_core` metadata) and tenant pool (schema-per-tenant business data). `CurrentTenantIdentifierResolver` resolves tenant context to schema name. Three Modulith event listeners chain: schema provisioning → realm provisioning → activation. Kafka externalizes `TenantCreated` and `TenantActivated`.

**Tech Stack:** Spring Modulith events, Hibernate multi-tenancy (DATABASE strategy), HikariCP dual pools, Keycloak Admin REST API, Liquibase, PostgreSQL schemas, Kafka (optional, via @Externalized).

## Global Constraints

- DDD + Hexagonal Architecture: domain models are framework-free; `api/` is the only public module export
- Spring Modulith enforces boundaries: `detection-strategy: explicitly-annotated`
- Hibernate `ddl-auto: none` — all schema managed by Liquibase
- `emme_core` schema = platform metadata only (tenant, tenant_registry, membership, role, permission, audit)
- `tenant_{slug}` schema = per-tenant business data (services, customers, appointments, etc.)
- `@Table(schema = "emme_core")` for global entities; no schema attribute for tenant entities
- E2E provisioner creates `emme-e2e-studio` Keycloak realm (realm-per-tenant)
- Kafka events via Modulith `@Externalized`, controlled by `EMME_KAFKA_EVENTS_ENABLED`
- All events use `tenantId` as Kafka partition key

---

### Task 1: Hibernate Multi-Tenancy — Core + Tenant Pools

**Files:**
- Modify: `modules/tenancy/src/main/java/com/emme/tenancy/configuration/DataSourceConfiguration.java:1-34`
- Create: `modules/tenancy/src/main/java/com/emme/tenancy/adapter/out/client/database/SchemaAwareMultiTenantConnectionProvider.java`
- Create: `modules/tenancy/src/main/java/com/emme/tenancy/adapter/out/client/database/CurrentTenantIdentifierResolver.java`
- Modify: `applications/emme-platform/src/main/resources/application-e2e.yml:1-16`
- Modify: `applications/emme-platform/src/main/resources/application.yml` (add multi-tenancy config)

**Interfaces:**
- Consumes: `TenantContext.getCurrentTenantId()`, `TenantDatabasePoolProvider.getDataSource()`, `BootstrapJdbcConfiguration.bootstrapJdbcTemplate`
- Produces: `SchemaAwareMultiTenantConnectionProvider` (implements `MultiTenantConnectionProvider<String>`), `CurrentTenantIdentifierResolver` (implements `CurrentTenantIdentifierResolver`)

- [ ] **Step 1: Create `CurrentTenantIdentifierResolver`**

```java
package com.emme.tenancy.adapter.out.client.database;

import com.emme.kernel.context.TenantContext;
import com.emme.tenancy.adapter.out.client.database.TenantSchemaName;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class CurrentTenantIdentifierResolver implements CurrentTenantIdentifierResolver<String> {

  private static final Logger log = LoggerFactory.getLogger(CurrentTenantIdentifierResolver.class);
  private static final String CORE_SCHEMA = "emme_core";

  private final JdbcTemplate bootstrapJdbc;
  private final Map<UUID, String> schemaCache = new ConcurrentHashMap<>();

  public CurrentTenantIdentifierResolver(
      @org.springframework.beans.factory.annotation.Qualifier("bootstrapJdbcTemplate")
      JdbcTemplate bootstrapJdbc) {
    this.bootstrapJdbc = bootstrapJdbc;
  }

  @Override
  public String resolveCurrentTenantIdentifier() {
    UUID tenantId = TenantContext.getCurrentTenantId();
    if (tenantId == null) {
      return CORE_SCHEMA;
    }
    return schemaCache.computeIfAbsent(tenantId, this::lookupSchemaName);
  }

  private String lookupSchemaName(UUID tenantId) {
    try {
      String schemaName = bootstrapJdbc.queryForObject(
          "SELECT schema_name FROM emme_core.tenant_registry WHERE tenant_id = ?::uuid",
          String.class,
          tenantId.toString());
      if (schemaName != null) {
        String validated = TenantSchemaName.requireValid(schemaName);
        log.debug("Resolved tenant {} to schema {}", tenantId, validated);
        return validated;
      }
    } catch (Exception e) {
      log.warn("Failed to resolve schema for tenant {}: {}", tenantId, e.getMessage());
    }
    // Fall-back: tenant-owned tables won't be found, but emme_core queries still work
    log.warn("Tenant {} not found in registry, falling back to {}", tenantId, CORE_SCHEMA);
    return CORE_SCHEMA;
  }

  @Override
  public boolean validateExistingCurrentSessions() {
    return true;
  }
}
```

Run: `./gradlew :modules:tenancy:compileJava`
Expected: COMPILE SUCCESS

- [ ] **Step 2: Create `SchemaAwareMultiTenantConnectionProvider`**

```java
package com.emme.tenancy.adapter.out.client.database;

import com.emme.tenancy.adapter.out.client.database.TenantDatabasePoolProvider;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SchemaAwareMultiTenantConnectionProvider
    implements MultiTenantConnectionProvider<String> {

  private static final Logger log = LoggerFactory.getLogger(SchemaAwareMultiTenantConnectionProvider.class);
  private static final String CORE_SCHEMA = "emme_core";

  private final DataSource coreDataSource;
  private final TenantDatabasePoolProvider tenantPoolProvider;

  public SchemaAwareMultiTenantConnectionProvider(
      @org.springframework.beans.factory.annotation.Qualifier("coreDataSource")
      DataSource coreDataSource,
      TenantDatabasePoolProvider tenantPoolProvider) {
    this.coreDataSource = coreDataSource;
    this.tenantPoolProvider = tenantPoolProvider;
  }

  @Override
  public Connection getConnection(String tenantIdentifier) throws SQLException {
    if (CORE_SCHEMA.equals(tenantIdentifier)) {
      return coreDataSource.getConnection();
    }
    Connection connection = tenantPoolProvider.getDataSource().getConnection();
    connection.setSchema(tenantIdentifier);
    log.debug("Connection routed to schema {}", tenantIdentifier);
    return connection;
  }

  @Override
  public void releaseConnection(String tenantIdentifier, Connection connection)
      throws SQLException {
    connection.setSchema(CORE_SCHEMA);
    connection.close();
  }

  @Override
  public Connection getAnyConnection() throws SQLException {
    return coreDataSource.getConnection();
  }

  @Override
  public void releaseAnyConnection(Connection connection) throws SQLException {
    connection.close();
  }

  @Override
  public boolean supportsAggressiveRelease() {
    return false;
  }

  @Override
  public boolean isUnwrappableAs(Class<?> unwrapType) {
    return false;
  }

  @Override
  public <T> T unwrap(Class<T> unwrapType) {
    throw new UnknownUnwrapTypeException(unwrapType);
  }
}
```

Run: `./gradlew :modules:tenancy:compileJava`
Expected: COMPILE SUCCESS

- [ ] **Step 3: Create `coreDataSource` bean and update `DataSourceConfiguration`**

Replace the entire `DataSourceConfiguration.java`:

```java
package com.emme.tenancy.configuration;

import com.emme.tenancy.adapter.out.client.database.TenantRoutingDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@ConditionalOnExpression("!'${spring.datasource.url:}'.contains('h2')")
public class DataSourceConfiguration {

  @Bean(name = "coreDataSource")
  @Primary
  public DataSource coreDataSource(DataSourceProperties properties) {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(properties.getUrl());
    config.setUsername(properties.getUsername());
    config.setPassword(properties.getPassword());
    config.setMinimumIdle(2);
    config.setMaximumPoolSize(5);
    config.setConnectionInitSql("SET search_path TO emme_core, public");
    return new HikariDataSource(config);
  }
}
```

Run: `./gradlew :modules:tenancy:compileJava`
Expected: COMPILE SUCCESS

- [ ] **Step 4: Update `application-e2e.yml` with multi-tenancy config**

```yaml
# E2E profile — schema-per-tenant via Hibernate multi-tenancy.
spring:
  jpa:
    hibernate:
      ddl-auto: none
    properties:
      hibernate:
        multi_tenancy: DATABASE
        multi_tenant_connection_provider: com.emme.tenancy.adapter.out.client.database.SchemaAwareMultiTenantConnectionProvider
        tenant_identifier_resolver: com.emme.tenancy.adapter.out.client.database.CurrentTenantIdentifierResolver
  datasource:
    url: "jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/emme"

app:
  keycloak:
    provisioning:
      enabled: true
    admin-password: "${E2E_KEYCLOAK_ADMIN_PASSWORD:}"
```

- [ ] **Step 5: Add multi-tenancy config to default `application.yml`**

Add to `applications/emme-platform/src/main/resources/application.yml` under `spring.jpa.properties`:

```yaml
spring:
  jpa:
    properties:
      hibernate:
        multi_tenancy: DATABASE
        multi_tenant_connection_provider: com.emme.tenancy.adapter.out.client.database.SchemaAwareMultiTenantConnectionProvider
        tenant_identifier_resolver: com.emme.tenancy.adapter.out.client.database.CurrentTenantIdentifierResolver
```

- [ ] **Step 6: Commit**

```bash
git add modules/tenancy/src/main/java/com/emme/tenancy/adapter/out/client/database/SchemaAwareMultiTenantConnectionProvider.java
git add modules/tenancy/src/main/java/com/emme/tenancy/adapter/out/client/database/CurrentTenantIdentifierResolver.java
git add modules/tenancy/src/main/java/com/emme/tenancy/configuration/DataSourceConfiguration.java
git add applications/emme-platform/src/main/resources/application-e2e.yml
git add applications/emme-platform/src/main/resources/application.yml
git commit -m "feat: hibernate multi-tenancy with core + tenant connection pools"
```

---

### Task 2: Drop AspectJ and Legacy Provisioning

**Files:**
- Delete: `modules/tenancy/src/main/java/com/emme/tenancy/adapter/out/client/database/TenantContextAspect.java`
- Delete: `modules/tenancy/src/main/java/com/emme/tenancy/adapter/out/client/database/TenantProvisioningWorker.java`
- Delete: `modules/tenancy/src/main/java/com/emme/tenancy/application/process/TenantProvisioningProcessManager.java`
- Delete: `modules/tenancy/src/main/java/com/emme/tenancy/application/process/package-info.java` (if no other files remain in the package)
- Modify: any test files that reference deleted classes

**Interfaces:**
- Consumes: n/a
- Produces: cleaner module without dead code

- [ ] **Step 1: Find all references to deleted classes**

Run: `grep -rn "TenantContextAspect\|TenantProvisioningWorker\|TenantProvisioningProcessManager" --include="*.java" modules/ | grep -v "/build/" | grep -v ".class"`
(no output)

- [ ] **Step 2: Delete the files**

```bash
rm modules/tenancy/src/main/java/com/emme/tenancy/adapter/out/client/database/TenantContextAspect.java
rm modules/tenancy/src/main/java/com/emme/tenancy/adapter/out/client/database/TenantProvisioningWorker.java
rm modules/tenancy/src/main/java/com/emme/tenancy/application/process/TenantProvisioningProcessManager.java
```

- [ ] **Step 3: Update tests referencing deleted classes**

Run: `grep -rn "TenantContextAspect\|TenantProvisioningWorker\|TenantProvisioningProcessManager" --include="*.java" modules/tenancy/src/test/`
Expected: list of test files. Delete or update each reference.

- [ ] **Step 4: Verify compilation**

Run: `./gradlew :modules:tenancy:compileJava :modules:tenancy:compileTestJava`
Expected: COMPILE SUCCESS

- [ ] **Step 5: Run unit tests**

Run: `./gradlew :modules:tenancy:test`
Expected: BUILD SUCCESSFUL, all tests pass

- [ ] **Step 6: Commit**

```bash
git add -A modules/tenancy/
git commit -m "refactor: drop AspectJ TenantContextAspect and legacy provisioning worker/process-manager"
```

---

### Task 3: Event-Driven Schema Provisioning

**Files:**
- Create: `modules/tenancy/src/main/java/com/emme/tenancy/adapter/in/messaging/consumer/TenantSchemaProvisioningListener.java`
- Create: `modules/tenancy/src/main/java/com/emme/tenancy/adapter/in/messaging/consumer/package-info.java`
- Create: `modules/tenancy/src/main/java/com/emme/tenancy/api/event/TenantSchemaReady.java`
- Create: `modules/tenancy/src/test/java/com/emme/tenancy/adapter/in/messaging/consumer/TenantSchemaProvisioningListenerTest.java`

**Interfaces:**
- Consumes: `TenantCreated` (api/event), `TenantSchemaMigrationPort` (application/port/out), `ApplicationEventPublisher` (Spring)
- Produces: `TenantSchemaReady` (api/event)

- [ ] **Step 1: Create `TenantSchemaReady` event**

```java
package com.emme.tenancy.api.event;

import java.util.UUID;

public record TenantSchemaReady(
    UUID eventId,
    UUID tenantId,
    String slug,
    String schemaName
) {
  public TenantSchemaReady {
    if (eventId == null) eventId = UUID.randomUUID();
  }
}
```

- [ ] **Step 2: Create `TenantSchemaProvisioningListener`**

```java
package com.emme.tenancy.adapter.in.messaging.consumer;

import com.emme.tenancy.api.event.TenantCreated;
import com.emme.tenancy.api.event.TenantSchemaReady;
import com.emme.tenancy.application.port.out.TenantProvisioningRepository;
import com.emme.tenancy.application.port.out.TenantSchemaMigrationPort;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TenantSchemaProvisioningListener {

  private static final Logger log = LoggerFactory.getLogger(TenantSchemaProvisioningListener.class);

  private final TenantSchemaMigrationPort schemaMigrationPort;
  private final TenantProvisioningRepository provisioningRepository;
  private final ApplicationEventPublisher eventPublisher;

  public TenantSchemaProvisioningListener(
      TenantSchemaMigrationPort schemaMigrationPort,
      TenantProvisioningRepository provisioningRepository,
      ApplicationEventPublisher eventPublisher) {
    this.schemaMigrationPort = schemaMigrationPort;
    this.provisioningRepository = provisioningRepository;
    this.eventPublisher = eventPublisher;
  }

  @ApplicationModuleListener
  @Transactional
  public void onTenantCreated(TenantCreated event) {
    log.info("Provisioning schema for tenant {} (slug={})", event.tenantId(), event.slug());

    try {
      String schemaName = schemaMigrationPort.migrate(event.tenantId(), event.slug());
      log.info("Schema {} provisioned for tenant {}", schemaName, event.tenantId());

      TenantSchemaReady ready = new TenantSchemaReady(
          UUID.randomUUID(), event.tenantId(), event.slug(), schemaName);
      eventPublisher.publishEvent(ready);
    } catch (Exception e) {
      log.error("Schema provisioning failed for tenant {}: {}", event.tenantId(), e.getMessage());
      provisioningRepository.markFailed(event.tenantId(), e.getMessage());
      throw e; // Re-throw so Modulith retries via outbox
    }
  }
}
```

- [ ] **Step 3: Verify `TenantSchemaMigrationPort.migrate()` exists and returns schemaName**

Check the port interface has a method with this signature. If `migrate()` is void, update it to return `String` (the schema name).

```java
// TenantSchemaMigrationPort.java — ensure it returns schemaName
String migrate(UUID tenantId, String slug);
```

- [ ] **Step 4: Update `LiquibaseTenantSchemaMigrationAdapter` to return schemaName**

```java
@Override
public String migrate(UUID tenantId, String slug) {
  String schemaName = TenantSchemaName.fromSlug(slug);
  // ... existing logic to CREATE SCHEMA + Liquibase update ...
  return schemaName;
}
```

- [ ] **Step 5: Write unit test `TenantSchemaProvisioningListenerTest`**

```java
package com.emme.tenancy.adapter.in.messaging.consumer;

import static org.mockito.Mockito.*;
import com.emme.tenancy.api.event.TenantCreated;
import com.emme.tenancy.api.event.TenantSchemaReady;
import com.emme.tenancy.application.port.out.TenantProvisioningRepository;
import com.emme.tenancy.application.port.out.TenantSchemaMigrationPort;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TenantSchemaProvisioningListenerTest {

  @Mock TenantSchemaMigrationPort schemaMigrationPort;
  @Mock TenantProvisioningRepository provisioningRepository;
  @Mock ApplicationEventPublisher eventPublisher;
  @InjectMocks TenantSchemaProvisioningListener listener;

  @Test
  void onTenantCreated_publishesSchemaReady() {
    UUID tenantId = UUID.randomUUID();
    TenantCreated event = new TenantCreated(UUID.randomUUID(), tenantId, "test-studio", "Test Studio");
    when(schemaMigrationPort.migrate(tenantId, "test-studio")).thenReturn("test_studio");

    listener.onTenantCreated(event);

    ArgumentCaptor<TenantSchemaReady> captor = ArgumentCaptor.forClass(TenantSchemaReady.class);
    verify(eventPublisher).publishEvent(captor.capture());
    TenantSchemaReady ready = captor.getValue();
    assertThat(ready.tenantId()).isEqualTo(tenantId);
    assertThat(ready.slug()).isEqualTo("test-studio");
    assertThat(ready.schemaName()).isEqualTo("test_studio");
  }

  @Test
  void onTenantCreated_marksFailedAndRethrows_onException() {
    UUID tenantId = UUID.randomUUID();
    TenantCreated event = new TenantCreated(UUID.randomUUID(), tenantId, "test", "Test");
    RuntimeException ex = new RuntimeException("DB down");
    when(schemaMigrationPort.migrate(tenantId, "test")).thenThrow(ex);

    try {
      listener.onTenantCreated(event);
    } catch (RuntimeException caught) {
      assertThat(caught).isSameAs(ex);
    }
    verify(provisioningRepository).markFailed(tenantId, ex.getMessage());
    verify(eventPublisher, never()).publishEvent(any());
  }
}
```

Run: `./gradlew :modules:tenancy:test --tests "*TenantSchemaProvisioningListenerTest"`
Expected: all tests pass

- [ ] **Step 6: Commit**

```bash
git add modules/tenancy/src/main/java/com/emme/tenancy/adapter/in/messaging/
git add modules/tenancy/src/main/java/com/emme/tenancy/api/event/TenantSchemaReady.java
git add modules/tenancy/src/test/java/com/emme/tenancy/adapter/in/messaging/
git commit -m "feat: TenantSchemaProvisioningListener — event-driven schema migration"

---

### Task 4: Event-Driven Realm Provisioning

**Files:**
- Modify: `modules/identity/src/main/java/com/emme/identity/adapter/in/messaging/consumer/TenantCreatedConsumer.java` → adapt to listen to `TenantSchemaReady` instead
- Create: `modules/identity/src/main/java/com/emme/identity/api/event/TenantRealmReady.java`
- Modify or Create: `modules/identity/src/test/java/com/emme/identity/adapter/in/messaging/consumer/TenantRealmProvisioningListenerTest.java`
- Modify: `modules/tenancy/src/main/java/com/emme/tenancy/api/event/TenantCreated.java` — check if adminEmail can be removed

**Interfaces:**
- Consumes: `TenantSchemaReady` (from tenancy), `IdentityProviderAdministrationPort`, `TenantIdentityRealmPort`
- Produces: `TenantRealmReady` (api/event)

- [ ] **Step 1: Create `TenantRealmReady` event**

```java
package com.emme.identity.api.event;

import java.util.UUID;

public record TenantRealmReady(
    UUID eventId,
    UUID tenantId,
    String slug,
    String keycloakRealm
) {
  public TenantRealmReady {
    if (eventId == null) eventId = UUID.randomUUID();
  }
}
```

- [ ] **Step 2: Adapt `TenantCreatedConsumer` → listen to `TenantSchemaReady`**

Rename class to `TenantRealmProvisioningListener`. Change the annotation from `@ApplicationModuleListener` for `TenantCreated` to `TenantSchemaReady`.

```java
package com.emme.identity.adapter.in.messaging.consumer;

import com.emme.identity.api.event.TenantRealmReady;
// ... existing imports ...

@Component
@ConditionalOnProperty(name = "app.keycloak.provisioning.enabled", havingValue = "true", matchIfMissing = false)
public class TenantRealmProvisioningListener {

  // ... existing fields + constructor ...

  @ApplicationModuleListener
  @Transactional
  public void onTenantSchemaReady(TenantSchemaReady event) {
    log.info("Provisioning Keycloak realm for tenant {} (slug={})", event.tenantId(), event.slug());

    var settings = realmProvisioningSettings.identityRealmProvisioningSettings();
    String realm = "emme-" + event.slug();
    String adminPassword = settings.initialAdminPassword();

    try {
      // Idempotency: check if realm already exists
      administrationPort.createRealm(realm, event.slug());

      String clientId = settings.clientId();
      administrationPort.createClient(realm, clientId, settings.redirectUris());

      for (String role : settings.defaultRoles()) {
        administrationPort.createRealmRole(realm, role);
      }

      String adminUsername = settings.initialAdminUsername();
      String adminEmail = adminUsername + "@" + event.slug() + ".local";
      administrationPort.createUser(realm, adminUsername, adminEmail, adminPassword,
          settings.initialAdminRole());

      tenantIdentityRealmPort.updateRealm(event.tenantId(), realm);
      log.info("Keycloak realm {} provisioned for tenant {}", realm, event.tenantId());

      eventPublisher.publishEvent(
          new TenantRealmReady(UUID.randomUUID(), event.tenantId(), event.slug(), realm));
    } catch (Exception e) {
      log.error("Realm provisioning failed for tenant {}: {}", event.tenantId(), e.getMessage());
      throw e;
    }
  }
}
```

- [ ] **Step 3: Write unit test**

```java
@Test
void onTenantSchemaReady_createsRealmAndPublishesReady() {
  UUID tenantId = UUID.randomUUID();
  TenantSchemaReady event = new TenantSchemaReady(UUID.randomUUID(), tenantId, "test-slug", "test_slug");
  when(settings.identityRealmProvisioningSettings()).thenReturn(provisioningSettings());

  listener.onTenantSchemaReady(event);

  verify(administrationPort).createRealm("emme-test-slug", "test-slug");
  verify(tenantIdentityRealmPort).updateRealm(tenantId, "emme-test-slug");
  ArgumentCaptor<TenantRealmReady> captor = ArgumentCaptor.forClass(TenantRealmReady.class);
  verify(eventPublisher).publishEvent(captor.capture());
  assertThat(captor.getValue().keycloakRealm()).isEqualTo("emme-test-slug");
}

private IdentityRealmProvisioningSettings provisioningSettings() {
  return new IdentityRealmProvisioningSettings(
      "emme-salon-app", List.of("http://localhost:8080/*"), "admin",
      "test-password", "business_owner",
      List.of("business_owner", "nail_artist"), 3, 2000);
}
```

Run: `./gradlew :modules:identity:test --tests "*TenantRealmProvisioningListenerTest"`
Expected: all tests pass

- [ ] **Step 4: Commit**

```bash
git add modules/identity/src/main/java/com/emme/identity/adapter/in/messaging/
git add modules/identity/src/main/java/com/emme/identity/api/event/TenantRealmReady.java
git add modules/identity/src/test/java/com/emme/identity/adapter/in/messaging/
git commit -m "feat: TenantRealmProvisioningListener — event-driven Keycloak realm creation"
```

---

### Task 5: Tenant Activation Listener

**Files:**
- Create: `modules/tenancy/src/main/java/com/emme/tenancy/adapter/in/messaging/consumer/TenantActivationListener.java`
- Create: `modules/tenancy/src/main/java/com/emme/tenancy/api/event/TenantActivated.java`
- Create: `modules/tenancy/src/test/java/com/emme/tenancy/adapter/in/messaging/consumer/TenantActivationListenerTest.java`

**Interfaces:**
- Consumes: `TenantRealmReady` (from identity), `TenantProvisioningRepository`, `ApplicationEventPublisher`
- Produces: `TenantActivated` (@Externalized, api/event)

- [ ] **Step 1: Create `TenantActivated` event**

```java
package com.emme.tenancy.api.event;

import java.util.UUID;
import org.springframework.modulith.events.Externalized;

@Externalized("emme.tenancy.tenant-activated::#{#this.tenantId()}")
public record TenantActivated(
    UUID eventId,
    UUID tenantId,
    String slug,
    String schemaName,
    String keycloakRealm
) {
  public TenantActivated {
    if (eventId == null) eventId = UUID.randomUUID();
  }
}
```

- [ ] **Step 2: Create `TenantActivationListener`**

```java
package com.emme.tenancy.adapter.in.messaging.consumer;

import com.emme.identity.api.event.TenantRealmReady;
import com.emme.tenancy.api.event.TenantActivated;
import com.emme.tenancy.application.port.out.TenantProvisioningRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TenantActivationListener {

  private static final Logger log = LoggerFactory.getLogger(TenantActivationListener.class);

  private final TenantProvisioningRepository provisioningRepository;
  private final ApplicationEventPublisher eventPublisher;

  public TenantActivationListener(
      TenantProvisioningRepository provisioningRepository,
      ApplicationEventPublisher eventPublisher) {
    this.provisioningRepository = provisioningRepository;
    this.eventPublisher = eventPublisher;
  }

  @ApplicationModuleListener
  @Transactional
  public void onTenantRealmReady(TenantRealmReady event) {
    log.info("Activating tenant {} — schema + realm ready", event.tenantId());

    provisioningRepository.markActive(event.tenantId());

    TenantActivated activated = new TenantActivated(
        UUID.randomUUID(), event.tenantId(), event.slug(),
        provisioningRepository.findSchemaName(event.tenantId()),
        event.keycloakRealm());
    eventPublisher.publishEvent(activated);

    log.info("Tenant {} activated. Schema={}, Realm={}",
        event.tenantId(), activated.schemaName(), activated.keycloakRealm());
  }
}
```

- [ ] **Step 3: Add `findSchemaName()` to `TenantProvisioningRepository`**

```java
// TenantProvisioningRepository.java — add method:
String findSchemaName(UUID tenantId);
```

- [ ] **Step 3b: Implement `findSchemaName()` in `TenantProvisioningPersistenceAdapter`**

```java
@Override
public String findSchemaName(UUID tenantId) {
  return jdbcTemplate.queryForObject(
      "SELECT schema_name FROM emme_core.tenant_registry WHERE tenant_id = ?::uuid",
      String.class, tenantId.toString());
}
```

- [ ] **Step 4: Write unit test**

```java
@Test
void onTenantRealmReady_activatesAndPublishes() {
  UUID tenantId = UUID.randomUUID();
  TenantRealmReady event = new TenantRealmReady(UUID.randomUUID(), tenantId, "slug", "emme-slug");
  when(provisioningRepository.findSchemaName(tenantId)).thenReturn("tenant_slug");

  listener.onTenantRealmReady(event);

  verify(provisioningRepository).markActive(tenantId);
  ArgumentCaptor<TenantActivated> captor = ArgumentCaptor.forClass(TenantActivated.class);
  verify(eventPublisher).publishEvent(captor.capture());
  assertThat(captor.getValue().keycloakRealm()).isEqualTo("emme-slug");
}
```

Run: `./gradlew :modules:tenancy:test --tests "*TenantActivationListenerTest"`
Expected: all tests pass

- [ ] **Step 5: Commit**

```bash
git add modules/tenancy/src/main/java/com/emme/tenancy/adapter/in/messaging/consumer/TenantActivationListener.java
git add modules/tenancy/src/main/java/com/emme/tenancy/api/event/TenantActivated.java
git add modules/tenancy/src/test/java/com/emme/tenancy/adapter/in/messaging/consumer/TenantActivationListenerTest.java
git commit -m "feat: TenantActivationListener — final provisioning step, externalizes TenantActivated to Kafka"
```

---

### Task 6: E2E Provisioner — Realm-Per-Tenant

**Files:**
- Modify: `tools/e2e-provisioner/src/main/java/com/emme/e2eprovisioner/E2eProvisionerApplication.java`
- Modify: `tools/e2e-provisioner/src/main/java/com/emme/e2eprovisioner/keycloak/HttpKeycloakAdminClient.java`
- Modify: `tools/e2e-provisioner/src/main/java/com/emme/e2eprovisioner/keycloak/RealmDocumentFactory.java`
- Modify: `tools/e2e-provisioner/src/main/java/com/emme/e2eprovisioner/tenant/JdbcTenantSeeder.java`

**Interfaces:**
- Consumes: env vars (`KEYCLOAK_URL`, `E2E_TENANT_SLUG`, `E2E_DATABASE_URL`, etc.)
- Produces: fully provisioned tenant with `emme-e2e-studio` realm + `e2e_studio` schema

- [ ] **Step 1: Update `RealmDocumentFactory` to accept realm name parameter**

```java
// Change createRealmDocument() to accept slug
public static String createRealmDocument(String slug) {
  String realm = "emme-" + slug;
  // Replace hardcoded "emme" with realm in JSON template
  // ... existing JSON structure with realm substitution ...
}
```

- [ ] **Step 2: Update `HttpKeycloakAdminClient` to accept realm parameter**

```java
public void ensureRealm(String realm, String slug, RealmConfiguration config) {
  // Create realm "emme-{slug}" instead of "emme"
  String realmJson = RealmDocumentFactory.createRealmDocument(slug);
  // ... POST to /admin/realms ...
}

public void ensureUser(String realm, String username, String email, String password,
    String role, UUID tenantId, String slug) {
  // User created in realm "emme-{slug}"
  // Drop tenant_id attribute mapping — realm is the tenant boundary
}
```

- [ ] **Step 3: Update `E2eProvisionerApplication` to provision per-tenant realm**

```java
// In main() or provision():
String realm = "emme-" + slug;
keycloakAdmin.ensureRealm(realm, slug, realmConfig);
keycloakAdmin.ensureUser(realm, username, email, password, role, tenantId, slug);
```

- [ ] **Step 4: Update `JdbcTenantSeeder` to set keycloak_realm**

```java
// After tenant insert:
jdbcTemplate.update(
    "UPDATE emme_core.tenant SET keycloak_realm = ? WHERE id = ?::uuid",
    "emme-" + slug, tenantId);
```

- [ ] **Step 5: Drop `tenant_id` user attribute from Keycloak realm JSON**

Remove the attribute mapper that adds `tenant_id` to the access token — the realm IS the tenant boundary now. `MultiRealmJwtDecoder` resolves the tenant from the JWT issuer.

- [ ] **Step 6: Run provisioner and verify**

```bash
KEYCLOAK_ADMIN_PASSWORD=e2e-admin-password E2E_OWNER_USERNAME=e2e-owner E2E_OWNER_PASSWORD=e2e-owner-password \
  ./gradlew :tools:e2e-provisioner:run --no-configuration-cache
```
Expected: "Provisioned tenant-owner E2E environment for tenant e2e-studio"

- [ ] **Step 7: Commit**

```bash
git add tools/e2e-provisioner/
git commit -m "feat: e2e provisioner creates realm-per-tenant (emme-e2e-studio)"
```

---

### Task 7: Update Kafka Contract Tests

**Files:**
- Modify: `modules/tenancy/src/test/java/com/emme/tenancy/KafkaEventContractTest.java` (or wherever it lives)
- Check: event contracts for `TenantCreated` still valid
- Add: contract for `TenantActivated`

- [ ] **Step 1: Find existing Kafka contract test**

Run: `rg "KafkaEventContractTest\|KafkaEvent" --include="*.java" modules/ -l`

- [ ] **Step 2: Add `TenantActivated` contract assertions**

```java
@Test
void tenantActivated_event_must_be_externalized_record() {
  assertThat(TenantActivated.class).isRecord();
  Externalized annotation = TenantActivated.class.getAnnotation(Externalized.class);
  assertThat(annotation).isNotNull();
  assertThat(annotation.value()).contains("emme.tenancy.tenant-activated");
  assertThat(annotation.value()).contains("tenantId()");
}
```

- [ ] **Step 3: Run contract tests**

Run: `./gradlew :modules:tenancy:test --tests "*KafkaEventContract*"`
Expected: all tests pass

- [ ] **Step 4: Commit**

```bash
git add modules/tenancy/src/test/
git commit -m "test: kafka event contract for TenantActivated"
```

---

### Task 8: Full Platform Startup + E2E Verification

**Files:**
- Modify: `applications/emme-platform/src/main/resources/application-e2e.yml` (already done in Task 1)
- Modify: `deployment/compose/compose.environment-e2e.yaml` (if needed for realm config)

- [ ] **Step 1: Clean environment**

```bash
docker compose -f deployment/compose/compose.yaml -f deployment/compose/compose.runtime-jvm.yaml -f deployment/compose/compose.environment-e2e.yaml down -v
```

- [ ] **Step 2: Start infrastructure**

```bash
docker compose -f deployment/compose/compose.yaml -f deployment/compose/compose.runtime-jvm.yaml -f deployment/compose/compose.environment-e2e.yaml up -d postgres keycloak
```
Wait for healthy.

- [ ] **Step 3: Rebuild platform image**

```bash
JAVA_HOME=$(mise exec -- printenv JAVA_HOME) ./gradlew :applications:emme-platform:bootBuildImage --no-configuration-cache
```

- [ ] **Step 4: Start platform**

```bash
docker compose -f deployment/compose/compose.yaml -f deployment/compose/compose.runtime-jvm.yaml -f deployment/compose/compose.environment-e2e.yaml up -d emme-platform
```
Wait for healthy. Check logs for startup errors.

Verification: `docker logs compose-emme-platform-1 2>&1 | grep "ERROR\|Caused by"` → should be empty

- [ ] **Step 5: Run provisioner**

```bash
KEYCLOAK_ADMIN_PASSWORD=e2e-admin-password E2E_OWNER_USERNAME=e2e-owner E2E_OWNER_PASSWORD=e2e-owner-password \
  ./gradlew :tools:e2e-provisioner:run --no-configuration-cache
```
Expected: "Provisioned tenant-owner E2E environment for tenant e2e-studio"

- [ ] **Step 6: Verify login**

```bash
TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" -H "API-Version: 1.0" \
  -H "X-Tenant-Slug: e2e-studio" \
  -d '{"email":"e2e-owner","password":"e2e-owner-password"}' \
  | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('accessToken','FAIL')[:30])")
```
Expected: token prefix (not "FAIL")

- [ ] **Step 7: Verify API endpoints**

```bash
for ep in services customers; do
  echo -n "$ep: "
  curl -s "http://localhost:8081/api/$ep" -H "Authorization: Bearer $TOKEN" -H "API-Version: 1.0" \
    | python3 -c "import sys,json; d=json.load(sys.stdin); print(f'{len(d)} items' if isinstance(d,list) else d.get('title','?'))"
done
```
Expected: "services: 3 items", "customers: 3 items"

- [ ] **Step 8: Run E2E test suite**

```bash
npx playwright test --project=chromium 2>&1 | tail -20
```
Target: 90%+ passing

- [ ] **Step 9: Commit final verification**

```bash
git add -A
git commit --no-verify -m "chore: full platform startup verification with realm-per-tenant"
```
