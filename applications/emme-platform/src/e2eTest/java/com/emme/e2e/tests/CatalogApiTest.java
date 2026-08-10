package com.emme.e2e.tests;

import static com.emme.client.E2eTest.withSession;
import static org.assertj.core.api.Assertions.assertThat;

import com.emme.client.E2eJson;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CatalogApiTest {

  @Test
  void shouldListCatalog() {
    withSession(
        s -> {
          var result = s.catalog().list();
          assertThat(result).isNotNull().startsWith("[");
        });
  }

  @Test
  void shouldCreateCatalogItem() {
    withSession(
        s -> {
          var serviceId =
              E2eJson.extract(
                  s.services()
                      .create(
                          "E2E Catalog Service",
                          "E2E-CATALOG-" + UUID.randomUUID().toString().substring(0, 8),
                          300,
                          60,
                          "E2E"),
                  "id");
          String uniqueCode = "E2E-" + UUID.randomUUID().toString().substring(0, 8);
          String body =
              """
                {"serviceId":"%s","code":"%s","name":"E2E Test Catalog Item","price":300.00}
                """
                  .formatted(serviceId, uniqueCode);
          var result = s.post("/api/catalog/items", body);
          assertThat(result).isNotNull();
        });
  }

  @Test
  void shouldMatchCatalog() {
    withSession(
        s -> {
          String body =
              """
                {"query":"E2E"}
                """;
          var result = s.post("/api/catalog/match", body, 200);
          assertThat(result).isNotNull();
        });
  }
}
