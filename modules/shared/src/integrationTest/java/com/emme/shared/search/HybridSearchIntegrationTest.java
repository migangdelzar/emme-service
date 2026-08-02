package com.emme.shared.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.TestApplication;
import com.emme.testing.integration.annotation.PostgresIntegrationTest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(classes = TestApplication.class)
@PostgresIntegrationTest
@DisplayName("hybrid search integration test")
class HybridSearchIntegrationTest {

  @Autowired private HybridSearch hybridSearch;

  @Autowired private JdbcTemplate jdbc;

  @AfterEach
  void cleanUpSearchFixture() {
    jdbc.execute("DROP TABLE IF EXISTS catalog_item");
  }

  @Test
  @DisplayName("search only returns active rows from the requested tenant")
  void searchIsTenantScopedAndAppliesTargetPredicates() {
    UUID tenantId = UUID.randomUUID();
    UUID otherTenantId = UUID.randomUUID();

    jdbc.execute(
        """
        CREATE TABLE catalog_item (
          id uuid PRIMARY KEY,
          tenant_id uuid NOT NULL,
          name varchar(200) NOT NULL,
          description varchar(2000),
          status varchar(10) NOT NULL,
          embedding bytea,
          search_tsv tsvector GENERATED ALWAYS AS
              (to_tsvector('spanish', name || ' ' || coalesce(description, ''))) STORED
        )
        """);
    jdbc.update(
        "INSERT INTO catalog_item (id, tenant_id, name, description, status) VALUES (?, ?, ?, ?, ?)",
        UUID.randomUUID(),
        tenantId,
        "Gel manicure",
        "Long lasting",
        "ACTIVE");
    jdbc.update(
        "INSERT INTO catalog_item (id, tenant_id, name, description, status) VALUES (?, ?, ?, ?, ?)",
        UUID.randomUUID(),
        tenantId,
        "Gel retired",
        "Do not show",
        "RETIRED");
    jdbc.update(
        "INSERT INTO catalog_item (id, tenant_id, name, description, status) VALUES (?, ?, ?, ?, ?)",
        UUID.randomUUID(),
        otherTenantId,
        "Gel other tenant",
        "Must not leak",
        "ACTIVE");

    List<HybridSearch.Scored> results =
        hybridSearch.search(SearchTarget.CATALOG_ITEM, tenantId, List.of(), "gel", 10);

    assertThat(results).hasSize(1);
  }

  @Test
  @DisplayName("embedding maintenance is tenant-scoped and honors the requested limit")
  void embeddingMaintenanceIsTenantScopedAndBounded() {
    UUID tenantId = UUID.randomUUID();
    UUID otherTenantId = UUID.randomUUID();
    UUID missingEmbeddingId = UUID.randomUUID();
    UUID secondMissingEmbeddingId = UUID.randomUUID();
    UUID otherTenantMissingEmbeddingId = UUID.randomUUID();
    UUID completedEmbeddingId = UUID.randomUUID();

    jdbc.execute(
        """
        CREATE TABLE catalog_item (
          id uuid PRIMARY KEY,
          tenant_id uuid NOT NULL,
          name varchar(200) NOT NULL,
          description varchar(2000),
          status varchar(10) NOT NULL,
          embedding bytea,
          search_tsv tsvector GENERATED ALWAYS AS
              (to_tsvector('spanish', name || ' ' || coalesce(description, ''))) STORED
        )
        """);
    jdbc.update(
        "INSERT INTO catalog_item (id, tenant_id, name, description, status, embedding) VALUES (?, ?, ?, ?, ?, ?)",
        missingEmbeddingId,
        tenantId,
        "Missing one",
        "Needs embedding",
        "ACTIVE",
        null);
    jdbc.update(
        "INSERT INTO catalog_item (id, tenant_id, name, description, status, embedding) VALUES (?, ?, ?, ?, ?, ?)",
        secondMissingEmbeddingId,
        tenantId,
        "Missing two",
        "Needs embedding",
        "ACTIVE",
        null);
    jdbc.update(
        "INSERT INTO catalog_item (id, tenant_id, name, description, status, embedding) VALUES (?, ?, ?, ?, ?, ?)",
        otherTenantMissingEmbeddingId,
        otherTenantId,
        "Other tenant",
        "Must not be returned",
        "ACTIVE",
        null);
    jdbc.update(
        "INSERT INTO catalog_item (id, tenant_id, name, description, status, embedding) VALUES (?, ?, ?, ?, ?, ?)",
        completedEmbeddingId,
        tenantId,
        "Completed",
        "Already embedded",
        "ACTIVE",
        new byte[] {1});

    UUID firstMissingEmbeddingId =
        List.of(missingEmbeddingId, secondMissingEmbeddingId).stream()
            .min(UUID::compareTo)
            .orElseThrow();

    assertThat(hybridSearch.idsMissingEmbedding(SearchTarget.CATALOG_ITEM, tenantId, 1))
        .containsExactly(firstMissingEmbeddingId);
    assertThat(hybridSearch.countMissingEmbedding(SearchTarget.CATALOG_ITEM, tenantId))
        .isEqualTo(2);
  }
}
