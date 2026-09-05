package com.emme.notification.adapter.out.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.notification.adapter.out.persistence.entity.NotificationEntity;
import com.emme.notification.adapter.out.persistence.mapper.NotificationPersistenceMapper;
import com.emme.notification.adapter.out.persistence.repository.SpringDataNotificationRepository;
import com.emme.notification.domain.model.Notification;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationPersistenceAdapterTest {

  @Test
  void listsNotificationsFromTheCurrentTenantSchema() {
    SpringDataNotificationRepository repository = org.mockito.Mockito.mock();
    NotificationPersistenceMapper mapper = new NotificationPersistenceMapper();
    NotificationPersistenceAdapter adapter = new NotificationPersistenceAdapter(repository, mapper);
    NotificationEntity entity =
        new NotificationEntity(
            UUID.randomUUID(), com.emme.kernel.type.NotificationChannel.EMAIL, "recipient", "body");
    entity.restoreIdentity(UUID.randomUUID(), Instant.now());
    when(repository.findAll()).thenReturn(List.of(entity));

    List<Notification> notifications = adapter.findAll();

    verify(repository).findAll();
    assertThat(notifications).hasSize(1);
  }
}
