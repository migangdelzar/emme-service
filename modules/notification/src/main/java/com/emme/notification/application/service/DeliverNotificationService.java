package com.emme.notification.application.service;

import com.emme.notification.api.command.DeliverNotificationCommand;
import com.emme.notification.api.result.NotificationInfo;
import com.emme.notification.api.usecase.DeliverNotificationUseCase;
import com.emme.notification.application.mapper.NotificationApplicationMapper;
import com.emme.notification.application.port.out.EmailSender;
import com.emme.notification.application.port.out.NotificationEventPublisher;
import com.emme.notification.application.port.out.NotificationRepository;
import com.emme.notification.application.port.out.PushSender;
import com.emme.notification.application.port.out.SmsSender;
import com.emme.notification.domain.model.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeliverNotificationService implements DeliverNotificationUseCase {
  private static final Logger log = LoggerFactory.getLogger(DeliverNotificationService.class);
  private final NotificationRepository repository;
  private final EmailSender emailSender;
  private final SmsSender smsSender;
  private final PushSender pushSender;
  private final NotificationEventPublisher events;

  public DeliverNotificationService(
      NotificationRepository repository,
      EmailSender emailSender,
      SmsSender smsSender,
      PushSender pushSender,
      NotificationEventPublisher events) {
    this.repository = repository;
    this.emailSender = emailSender;
    this.smsSender = smsSender;
    this.pushSender = pushSender;
    this.events = events;
  }

  @Override
  public NotificationInfo deliver(DeliverNotificationCommand command) {
    Notification notification =
        NotificationServiceSupport.load(repository, command.tenantId(), command.notificationId());
    if (notification.status() == com.emme.notification.domain.model.NotificationStatus.DELIVERED) {
      return NotificationApplicationMapper.toInfo(notification);
    }
    try {
      String providerId = send(notification);
      notification.markSent();
      notification.markDelivered();
      var saved = repository.save(notification);
      events.publish(new com.emme.notification.api.event.NotificationDelivered(saved.body()));
      log.info(
          "Notification delivered: channel={}, providerId={}", notification.channel(), providerId);
      return NotificationApplicationMapper.toInfo(saved);
    } catch (Exception exception) {
      log.error(
          "Notification delivery failed: notificationId={}", command.notificationId(), exception);
      notification.markFailed();
      return NotificationApplicationMapper.toInfo(repository.save(notification));
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
