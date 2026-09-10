package com.emme.notification.application.service;

import com.emme.notification.api.command.DeliverNotificationCommand;
import com.emme.notification.api.result.NotificationDetails;
import com.emme.notification.api.usecase.DeliverNotificationUseCase;
import com.emme.notification.application.mapper.NotificationApplicationMapper;
import com.emme.notification.application.port.out.EmailSender;
import com.emme.notification.application.port.out.NotificationEventPublisher;
import com.emme.notification.application.port.out.NotificationRepository;
import com.emme.notification.application.port.out.PushSender;
import com.emme.notification.application.port.out.SmsSender;
import com.emme.notification.domain.model.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class DeliverNotificationService implements DeliverNotificationUseCase {
  private final NotificationRepository repository;
  private final EmailSender emailSender;
  private final SmsSender smsSender;
  private final PushSender pushSender;
  private final NotificationEventPublisher events;

  @Override
  public NotificationDetails deliver(DeliverNotificationCommand command) {
    Notification notification =
        NotificationServiceSupport.load(repository, command.notificationId());
    if (notification.status() == com.emme.notification.domain.model.NotificationStatus.DELIVERED) {
      return NotificationApplicationMapper.toResult(notification);
    }
    try {
      String providerId = send(notification);
      notification.markSent();
      notification.markDelivered();
      var saved = repository.save(notification);
      events.publish(new com.emme.notification.api.event.NotificationDelivered(saved.body()));
      log.info(
          "Notification delivered: channel={}, providerId={}", notification.channel(), providerId);
      return NotificationApplicationMapper.toResult(saved);
    } catch (Exception exception) {
      log.error(
          "Notification delivery failed: notificationId={}", command.notificationId(), exception);
      notification.markFailed();
      return NotificationApplicationMapper.toResult(repository.save(notification));
    }
  }

  private String send(Notification notification) {
    return switch (notification.channel()) {
      case EMAIL ->
          emailSender.send(
              notification.recipientReference(), "EMME Notification", notification.body(), null);
      case SMS -> smsSender.send(notification.recipientReference(), notification.body());
      case PUSH ->
          pushSender.send(
              notification.recipientReference(), "EMME", notification.body(), java.util.Map.of());
      case WHATSAPP, WEB ->
          throw new IllegalStateException(
              "No notification provider configured for channel: " + notification.channel());
    };
  }
}
