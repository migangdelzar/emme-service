package com.emme.configuration;

import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

/**
 * Seeds demo data into the demo tenant schema. Only active with local profile. NOT part of
 * Liquibase migrations.
 */
@Component
@Profile("local")
public class DataSeeder implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

  private final DataSource dataSource;

  public DataSeeder(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public void run(String... args) {
    try (var conn = dataSource.getConnection()) {
      conn.setSchema("demo_tenant");
      try (var stmt = conn.createStatement()) {
        // Only seed if empty
        var rs = stmt.executeQuery("SELECT count(*) FROM customer");
        rs.next();
        if (rs.getInt(1) > 0) {
          log.info("Demo data already exists, skipping seed");
          return;
        }
      }

      var populator = new ResourceDatabasePopulator(new ClassPathResource("db/demo/data.sql"));
      populator.populate(conn);
      log.info("Demo data seeded successfully");
    } catch (Exception e) {
      log.warn("Could not seed demo data: {}", e.getMessage());
    }
  }
}
