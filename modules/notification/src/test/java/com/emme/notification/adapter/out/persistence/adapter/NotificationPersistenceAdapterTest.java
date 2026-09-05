package com.emme.notification.adapter.out.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.kernel.type.NotificationChannel;
import com.emme.notification.adapter.out.persistence.entity.NotificationEntity;
import com.emme.notification.adapter.out.persistence.mapper.NotificationPersistenceMapper;
import com.emme.notification.adapter.out.persistence.repository.SpringDataNotificationRepository;
import com.emme.notification.domain.model.Notification;
import com.emme.notification.domain.model.NotificationStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationPersistenceAdapterTest {

  @Test
  void listsNotificationsFromTheCurrentTenantSchema() {
    SpringDataNotificationRepository repository = mock();
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

  @Test
  void updatesTheManagedEntityWhenSavingAnExistingNotification() {
    SpringDataNotificationRepository repository = mock();
    NotificationPersistenceMapper mapper = new NotificationPersistenceMapper();
    NotificationPersistenceAdapter adapter = new NotificationPersistenceAdapter(repository, mapper);
    UUID notificationId = UUID.randomUUID();
    Instant createdAt = Instant.now();
    Notification notification =
        Notification.rehydrate(
            notificationId,
            UUID.randomUUID(),
            NotificationChannel.EMAIL,
            "recipient",
            "body",
            NotificationStatus.REQUESTED,
            createdAt);
    notification.markCancelled();
    NotificationEntity managedEntity =
        new NotificationEntity(
            notification.tenantId(), notification.channel(), "recipient", "body");
    managedEntity.restoreIdentity(notificationId, createdAt);
    when(repository.findById(notificationId)).thenReturn(Optional.of(managedEntity));
    when(repository.save(managedEntity)).thenReturn(managedEntity);

    Notification saved = adapter.save(notification);

    verify(repository).findById(notificationId);
    verify(repository).save(managedEntity);
    assertThat(saved.status()).isEqualTo(NotificationStatus.CANCELLED);
  }
}
