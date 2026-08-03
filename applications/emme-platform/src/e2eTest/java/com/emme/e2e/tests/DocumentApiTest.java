package com.emme.e2e.tests;

import static com.emme.client.E2eTest.withSession;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DocumentApiTest {

  @Test
  void shouldListDocuments() {
    withSession(
        s -> {
          var result = s.documents().list();
          assertThat(result).isNotNull().startsWith("[");
        });
  }

  @Test
  void shouldCreateDocument() {
    withSession(
        s -> {
          String body =
              """
                {"name":"E2E Test Document","sourceType":"NOTE"}
                """;
          var result = s.post("/api/documents", body, 500);
          assertThat(result).isNotNull();
        });
  }
}
