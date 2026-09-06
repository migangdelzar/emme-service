package com.emme;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StableApplicationListenerIdentityTest {

  @Test
  void durableModuleListenersDeclareStableIds() throws IOException {
    Map<String, List<String>> expectedIds =
        Map.of(
            "modules/calendar/src/main/java/com/emme/calendar/adapter/in/messaging/CalendarSyncListener.java",
                List.of(
                    "calendar.appointment-created",
                    "calendar.appointment-cancelled",
                    "calendar.appointment-rescheduled"),
            "modules/calendar/src/main/java/com/emme/calendar/adapter/out/google/adapter/StaffCalendarSyncAdapter.java",
                List.of("calendar.sync-requested.staff"),
            "modules/tenancy/src/main/java/com/emme/tenancy/adapter/in/messaging/consumer/TenantSchemaProvisioningListener.java",
                List.of("tenancy.tenant-created.schema-provisioning"),
            "modules/identity/src/main/java/com/emme/identity/adapter/in/messaging/consumer/TenantRealmProvisioningListener.java",
                List.of("identity.tenant-schema-ready.realm-provisioning"),
            "modules/tenancy/src/main/java/com/emme/tenancy/adapter/in/messaging/consumer/TenantActivationListener.java",
                List.of("tenancy.tenant-realm-ready.activation"),
            "modules/subscriptions/src/main/java/com/emme/subscriptions/adapter/in/messaging/consumer/SubscriptionProvisioningListener.java",
                List.of("subscriptions.tenant-activated.provisioning"));

    expectedIds.forEach(
        (path, listenerIds) -> {
          String source = readUnchecked(path);
          listenerIds.forEach(
              listenerId ->
                  assertThat(source)
                      .as("listener source %s", path)
                      .contains("id = \"" + listenerId + "\""));
        });
  }

  private static String read(String relativePath) throws IOException {
    Path current = Path.of("").toAbsolutePath();
    while (current != null) {
      Path candidate = current.resolve(relativePath);
      if (Files.exists(candidate)) {
        return Files.readString(candidate);
      }
      current = current.getParent();
    }
    throw new IllegalStateException("Cannot locate source path: " + relativePath);
  }

  private static String readUnchecked(String relativePath) {
    try {
      return read(relativePath);
    } catch (IOException exception) {
      throw new IllegalStateException("Cannot read source path: " + relativePath, exception);
    }
  }
}
