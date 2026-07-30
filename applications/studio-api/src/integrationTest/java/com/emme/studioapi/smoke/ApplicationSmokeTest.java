package com.emme.studioapi.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.TestApplication;
import com.emme.testing.integration.annotation.PostgresIntegrationTest;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = TestApplication.class)
@PostgresIntegrationTest
@DisplayName("Studio API smoke tests")
class ApplicationSmokeTest {

  @Autowired private DataSource dataSource;

  @Test
  @DisplayName("Assembled application boots with PostgreSQL")
  void applicationBoots() throws Exception {
    try (var conn = dataSource.getConnection()) {
      var stmt = conn.createStatement();
      var rs = stmt.executeQuery("SELECT 1");
      assertThat(rs.next()).isTrue();
    }
  }
}
