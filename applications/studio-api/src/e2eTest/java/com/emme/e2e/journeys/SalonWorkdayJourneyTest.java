package com.emme.e2e.journeys;

import static com.emme.client.E2eTest.withSession;
import static com.emme.client.E2eTest.withUnauthenticated;
import static org.assertj.core.api.Assertions.assertThat;

import com.emme.client.Role;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SalonWorkdayJourneyTest {
  private static final String DEMO_TENANT = "00000000-0000-0000-0000-100000000000";

  @Test
  void doHealthCheck() {
    withUnauthenticated(
        s -> {
          var health = s.get("/actuator/health");
          assertThat(health).contains("UP");
        });
  }

  @Test
  void doCreateCustomerAndVerify() {
    withSession(
        DEMO_TENANT,
        Role.BUSINESS_OWNER,
        s -> {
          s.setup().subscription(DEMO_TENANT);
          var user = s.user();
          String uniqueName = user.name() + " " + UUID.randomUUID().toString().substring(0, 6);
          var customerJson =
              s.customers()
                  .create(uniqueName, user.email(), "555-" + user.tenantId().substring(0, 4));
          assertThat(customerJson).isNotNull();
          var listed = s.customers().list();
          assertThat(listed).isNotNull();
        });
  }

  @Test
  void doCreateServiceAndVerify() {
    withSession(
        DEMO_TENANT,
        Role.BUSINESS_OWNER,
        s -> {
          s.setup().subscription(DEMO_TENANT);
          String uniqueName = UUID.randomUUID().toString().substring(0, 8) + "-Journey-Manicure";
          var serviceJson =
              s.services()
                  .create(
                      uniqueName,
                      "E2E-"
                          + uniqueName
                              .substring(0, Math.min(uniqueName.length(), 20))
                              .replaceAll("[^a-zA-Z0-9]", "-"),
                      750,
                      60,
                      "Manicura");
          assertThat(serviceJson).isNotNull();
          var listed = s.services().list();
          assertThat(listed).isNotNull();
        });
  }

  @Test
  void doFullBusinessDay() {
    withSession(
        DEMO_TENANT,
        Role.BUSINESS_OWNER,
        s -> {
          s.setup().subscription(DEMO_TENANT);
          var user = s.user();

          // 1. Health
          assertThat(s.get("/actuator/health")).contains("UP");

          // 2. Create customer
          String uniqueCustName = user.name() + " " + UUID.randomUUID().toString().substring(0, 6);
          var customerJson =
              s.customers()
                  .create(uniqueCustName, user.email(), "555-" + user.tenantId().substring(0, 4));
          assertThat(customerJson).isNotNull();

          // 3. Create service
          String uniqueSvcName = UUID.randomUUID().toString().substring(0, 8) + "-Journey-Soft-Gel";
          var serviceJson =
              s.services()
                  .create(
                      uniqueSvcName,
                      "E2E-"
                          + uniqueSvcName
                              .substring(0, Math.min(uniqueSvcName.length(), 20))
                              .replaceAll("[^a-zA-Z0-9]", "-"),
                      500,
                      90,
                      "Extensiones");
          assertThat(serviceJson).isNotNull();

          // 4. Verify listings
          assertThat(s.customers().list()).isNotNull();
          assertThat(s.services().list()).isNotNull();

          // 5. API docs
          assertThat(s.get("/api-docs")).contains("openapi");

          // 6. Health again
          assertThat(s.get("/actuator/health")).contains("UP");
        });
  }
}
