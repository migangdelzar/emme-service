package com.emme.e2e.tests;

import static com.emme.client.E2eTest.withUnauthenticated;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Verifies that the deployed OpenAPI document retains the reviewed route families. */
class OpenApiContractTest {

  @Test
  void shouldExposeTheReviewedRouteFamilies() {
    withUnauthenticated(
        session -> {
          var document = session.get("/api-docs");

          assertThat(document).contains("\"openapi\"");
          requiredPaths().forEach(path -> assertThat(document).contains("\"" + path + "\""));
        });
  }

  private static java.util.List<String> requiredPaths() {
    try (var stream =
        OpenApiContractTest.class
            .getClassLoader()
            .getResourceAsStream("contracts/openapi-required-paths.txt")) {
      assertThat(stream).as("OpenAPI contract manifest").isNotNull();
      try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
        return reader
            .lines()
            .map(String::trim)
            .filter(line -> !line.isEmpty() && !line.startsWith("#"))
            .collect(Collectors.toList());
      }
    } catch (IOException exception) {
      throw new IllegalStateException("Cannot read OpenAPI contract manifest", exception);
    }
  }
}
