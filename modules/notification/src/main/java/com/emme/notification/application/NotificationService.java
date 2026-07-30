package com.emme.notification.application;

import com.emme.kernel.type.NotificationChannel;
import com.emme.notification.entity.Notification;
import com.emme.notification.entity.NotificationRepository;
import com.emme.notification.event.NotificationDeliveredEvent;
import com.emme.notification.provider.EmailProvider;
import com.emme.notification.provider.PushProvider;
import com.emme.notification.provider.SmsProvider;
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

  private final NotificationRepository notificationRepository;
  private final EmailProvider emailProvider;
  private final SmsProvider smsProvider;
  private final PushProvider pushProvider;
  private final ApplicationEventPublisher events;

  public NotificationService(
      NotificationRepository notificationRepository,
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
  public Notification request(
      UUID tenantId, NotificationChannel channel, String recipient, String message) {
    Notification notification = new Notification(tenantId, channel, recipient, message);
    log.info(
        "Notification requested: channel={}, recipient={}, message={}",
        channel,
        recipient,
        message);
    return notificationRepository.save(notification);
  }

  /** Deliver a notification: dispatches to appropriate provider based on channel */
  public Notification deliver(UUID notificationId) {
    Notification n = findNotificationOrThrow(notificationId);
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
  public Notification cancel(UUID notificationId) {
    Notification notification = findNotificationOrThrow(notificationId);
    notification.markCancelled();
    log.info("Notification cancelled: {}", notificationId);
    return notificationRepository.save(notification);
  }

  @Transactional(readOnly = true)
  public List<Notification> findByTenantId(UUID tenantId) {
    return notificationRepository.findByTenantId(tenantId);
  }

  @Transactional(readOnly = true)
  public Notification findById(UUID notificationId) {
    return findNotificationOrThrow(notificationId);
  }

  private Notification findNotificationOrThrow(UUID notificationId) {
    return notificationRepository
        .findById(notificationId)
        .orElseThrow(
            () -> new IllegalArgumentException("Notification not found: " + notificationId));
  }
}
