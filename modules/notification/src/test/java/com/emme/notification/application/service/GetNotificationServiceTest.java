package com.emme.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.kernel.type.NotificationChannel;
import com.emme.notification.api.query.GetNotificationQuery;
import com.emme.notification.application.port.out.NotificationRepository;
import com.emme.notification.domain.model.Notification;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetNotificationServiceTest {

  @Test
  void loadsANotificationByIdFromTheTenantScopedConnection() {
    UUID recordTenant = UUID.randomUUID();
    Notification notification =
        new Notification(recordTenant, NotificationChannel.EMAIL, "recipient", "body");
    GetNotificationService service = new GetNotificationService(new Repository(notification));

    Optional<?> result = service.get(new GetNotificationQuery(recordTenant, notification.id()));

    assertThat(result).isPresent();
  }

  private static final class Repository implements NotificationRepository {
    private final Notification notification;

    private Repository(Notification notification) {
      this.notification = notification;
    }

    @Override
    public Optional<Notification> findById(UUID notificationId) {
      return java.util.Objects.equals(notification.id(), notificationId)
          ? Optional.of(notification)
          : Optional.empty();
    }

    @Override
    public List<Notification> findAll() {
      return List.of();
    }

    @Override
    public Notification save(Notification notification) {
      return notification;
    }
  }
}
