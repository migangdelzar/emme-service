package com.emme.shared.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.TestApplication;
import com.emme.testing.integration.annotation.PostgresIntegrationTest;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = TestApplication.class)
@PostgresIntegrationTest
@DisplayName("hybrid search integration test")
class HybridSearchIntegrationTest {

  @Autowired private DataSource dataSource;

  @Autowired private HybridSearch hybridSearch;

  @Test
  @DisplayName("search only returns active rows from the requested tenant")
  void searchIsTenantScopedAndAppliesTargetPredicates() {
    UUID tenantId = UUID.randomUUID();
    UUID otherTenantId = UUID.randomUUID();

    var jdbc = new org.springframework.jdbc.core.JdbcTemplate(dataSource);
    jdbc.execute(
        """
        CREATE TABLE catalog_item (
          id uuid PRIMARY KEY,
          tenant_id uuid NOT NULL,
          name varchar(200) NOT NULL,
          description varchar(2000),
          status varchar(10) NOT NULL,
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
}
