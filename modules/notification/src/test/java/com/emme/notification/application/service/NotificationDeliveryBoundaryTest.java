package com.emme.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emme.kernel.type.NotificationChannel;
import com.emme.notification.api.command.DeliverNotificationCommand;
import com.emme.notification.application.port.out.EmailSender;
import com.emme.notification.application.port.out.NotificationEventPublisher;
import com.emme.notification.application.port.out.NotificationRepository;
import com.emme.notification.application.port.out.PushSender;
import com.emme.notification.application.port.out.SmsSender;
import com.emme.notification.domain.model.Notification;
import com.emme.notification.domain.model.NotificationStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationDeliveryBoundaryTest {

  @Test
  void doesNotRedeliverAnAlreadyDeliveredNotification() {
    UUID tenantId = UUID.randomUUID();
    UUID notificationId = UUID.randomUUID();
    Notification notification =
        Notification.rehydrate(
            notificationId,
            tenantId,
            NotificationChannel.EMAIL,
            "recipient",
            "body",
            NotificationStatus.DELIVERED,
            Instant.now());
    NotificationRepository repository = mock(NotificationRepository.class);
    EmailSender emailSender = mock(EmailSender.class);
    SmsSender smsSender = mock(SmsSender.class);
    PushSender pushSender = mock(PushSender.class);
    NotificationEventPublisher events = mock(NotificationEventPublisher.class);
    when(repository.findByTenantIdAndId(tenantId, notificationId))
        .thenReturn(Optional.of(notification));

    var result =
        new DeliverNotificationService(repository, emailSender, smsSender, pushSender, events)
            .deliver(new DeliverNotificationCommand(tenantId, notificationId));

    assertThat(result.status()).isEqualTo(NotificationStatus.DELIVERED);
    verifyNoInteractions(emailSender, smsSender, pushSender, events);
  }

  @Test
  void doesNotAllowCancellationToLoadAnotherTenantNotification() {
    UUID tenantId = UUID.randomUUID();
    UUID notificationId = UUID.randomUUID();
    NotificationRepository repository = mock(NotificationRepository.class);
    when(repository.findByTenantIdAndId(tenantId, notificationId)).thenReturn(Optional.empty());

    var service = new CancelNotificationService(repository);

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                service.cancel(
                    new com.emme.notification.api.command.CancelNotificationCommand(
                        tenantId, notificationId)))
        .isInstanceOf(com.emme.notification.api.exception.NotificationNotFoundException.class);
  }

  @Test
  void marksUnsupportedChannelsAsFailedInsteadOfDelivered() {
    UUID tenantId = UUID.randomUUID();
    UUID notificationId = UUID.randomUUID();
    Notification notification =
        Notification.rehydrate(
            notificationId,
            tenantId,
            NotificationChannel.WHATSAPP,
            "recipient",
            "body",
            NotificationStatus.REQUESTED,
            Instant.now());
    NotificationRepository repository = mock(NotificationRepository.class);
    EmailSender emailSender = mock(EmailSender.class);
    SmsSender smsSender = mock(SmsSender.class);
    PushSender pushSender = mock(PushSender.class);
    NotificationEventPublisher events = mock(NotificationEventPublisher.class);
    when(repository.findByTenantIdAndId(tenantId, notificationId))
        .thenReturn(Optional.of(notification));
    when(repository.save(notification)).thenReturn(notification);

    var result =
        new DeliverNotificationService(repository, emailSender, smsSender, pushSender, events)
            .deliver(new DeliverNotificationCommand(tenantId, notificationId));

    assertThat(result.status()).isEqualTo(NotificationStatus.FAILED);
    verifyNoInteractions(emailSender, smsSender, pushSender, events);
  }
}
