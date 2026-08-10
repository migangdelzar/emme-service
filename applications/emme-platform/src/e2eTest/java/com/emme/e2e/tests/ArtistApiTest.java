package com.emme.e2e.tests;

import static com.emme.client.E2eTest.withSession;
import static org.assertj.core.api.Assertions.assertThat;

import com.emme.client.E2eJson;
import org.junit.jupiter.api.Test;

class ArtistApiTest {

  @Test
  void shouldListArtists() {
    withSession(
        s -> {
          var result = s.artists().list();
          assertThat(result).isNotNull().startsWith("[");
        });
  }

  @Test
  void shouldCreateAndGetArtistById() {
    withSession(
        s -> {
          var createResult = s.artists().create("E2E Test Artist");
          assertThat(createResult).isNotNull().contains("\"name\":\"E2E Test Artist\"");

          var id = E2eJson.extract(createResult, "id");
          assertThat(id).isNotNull();

          var getResult = s.artists().getById(id);
          assertThat(getResult).isNotNull().contains(id);
        });
  }

  @Test
  void shouldReturnNotFoundForUnknownId() {
    withSession(
        s -> {
          var result = s.get("/api/artists/00000000-0000-0000-0000-000000000000", 404);
          assertThat(result).isNotNull();
        });
  }
}
