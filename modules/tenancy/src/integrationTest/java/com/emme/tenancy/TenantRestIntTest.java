package com.emme.tenancy;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.TestApplication;
import com.emme.testing.integration.annotation.PostgresIntegrationTest;
import java.sql.Connection;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Pilot integration test proving:
 *
 * <ul>
 *   <li>PostgreSQL 16 container starts via {@code @ServiceConnection}
 *   <li>Spring context boots with {@link TestApplication}
 *   <li>JDBC connection works against the Testcontainers PostgreSQL
 * </ul>
 */
@SpringBootTest(classes = TestApplication.class)
@PostgresIntegrationTest
@DisplayName("Tenancy PostgreSQL integration test pilot")
class TenantRestIntTest {

  @Autowired private DataSource dataSource;

  @Test
  @DisplayName("PostgreSQL container is wired via @ServiceConnection")
  void postgresContainerIsWired() throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      var stmt = conn.createStatement();
      var rs = stmt.executeQuery("SELECT 1");
      assertThat(rs.next()).isTrue();
      assertThat(rs.getInt(1)).isEqualTo(1);
    }
  }

  @Test
  @DisplayName("Spring context boots successfully")
  void contextLoads() {
    assertThat(dataSource).isNotNull();
  }
}
