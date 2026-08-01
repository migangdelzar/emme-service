package com.emme.notification.application.service;

import com.emme.kernel.type.NotificationChannel;
import com.emme.notification.adapter.out.persistence.entity.NotificationEntity;
import com.emme.notification.adapter.out.persistence.repository.SpringDataNotificationRepository;
import com.emme.notification.adapter.out.provider.EmailProvider;
import com.emme.notification.adapter.out.provider.PushProvider;
import com.emme.notification.adapter.out.provider.SmsProvider;
import com.emme.notification.api.event.NotificationDeliveredEvent;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationService {

  private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

  private final SpringDataNotificationRepository notificationRepository;
  private final EmailProvider emailProvider;
  private final SmsProvider smsProvider;
  private final PushProvider pushProvider;
  private final ApplicationEventPublisher events;

  public NotificationService(
      SpringDataNotificationRepository notificationRepository,
      EmailProvider emailProvider,
      SmsProvider smsProvider,
      PushProvider pushProvider,
      ApplicationEventPublisher events) {
    this.notificationRepository = notificationRepository;
    this.emailProvider = emailProvider;
    this.smsProvider = smsProvider;
    this.pushProvider = pushProvider;
    this.events = events;
  }

  /** Request a notification to be sent. Status: REQUESTED */
  public NotificationEntity request(
      UUID tenantId, NotificationChannel channel, String recipient, String message) {
    NotificationEntity notification = new NotificationEntity(tenantId, channel, recipient, message);
    log.info(
        "Notification requested: channel={}, recipient={}, message={}",
        channel,
        recipient,
        message);
    return notificationRepository.save(notification);
  }

  /** Deliver a notification: dispatches to appropriate provider based on channel */
  public NotificationEntity deliver(UUID notificationId) {
    NotificationEntity n = findNotificationOrThrow(notificationId);
    try {
      String resultId =
          switch (n.getChannel()) {
            case EMAIL ->
                emailProvider.send(
                    n.getRecipientReference(), "EMME Notification", n.getBody(), null);
            case SMS -> smsProvider.send(n.getRecipientReference(), n.getBody());
            case PUSH ->
                pushProvider.send(
                    n.getRecipientReference(), "EMME", n.getBody(), java.util.Map.of());
            case WHATSAPP, WEB -> null; // handled by WhatsAppMessageService
          };
      n.markSent();
      n.markDelivered();
      log.info("Notification delivered: channel={}, providerId={}", n.getChannel(), resultId);
      events.publishEvent(new NotificationDeliveredEvent(n.getBody()));
      return notificationRepository.save(n);
    } catch (Exception e) {
      log.error(
          "Notification delivery failed: notificationId={}, channel={}",
          notificationId,
          n.getChannel(),
          e);
      n.markFailed();
      return notificationRepository.save(n);
    }
  }

  /** Cancel a notification: REQUESTED → CANCELLED */
  public NotificationEntity cancel(UUID notificationId) {
    NotificationEntity notification = findNotificationOrThrow(notificationId);
    notification.markCancelled();
    log.info("Notification cancelled: {}", notificationId);
    return notificationRepository.save(notification);
  }

  @Transactional(readOnly = true)
  public List<NotificationEntity> findByTenantId(UUID tenantId) {
    return notificationRepository.findByTenantId(tenantId);
  }

  @Transactional(readOnly = true)
  public NotificationEntity findById(UUID notificationId) {
    return findNotificationOrThrow(notificationId);
  }

  private NotificationEntity findNotificationOrThrow(UUID notificationId) {
    return notificationRepository
        .findById(notificationId)
        .orElseThrow(
            () -> new IllegalArgumentException("Notification not found: " + notificationId));
  }
}
