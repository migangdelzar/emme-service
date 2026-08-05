package com.emme.appointments.adapter.in.web.sse;

import com.emme.notification.api.event.NotificationDelivered;
import com.emme.appointments.api.event.AppointmentCancelled;
import com.emme.appointments.api.event.AppointmentCreated;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Inbound web adapter that projects module events to dashboard SSE subscribers. */
@Component
public class DashboardBroadcaster {

  private static final Logger log = LoggerFactory.getLogger(DashboardBroadcaster.class);
  private static final int MAX_SUBSCRIBERS = 100;

  private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

  @ApplicationModuleListener
  public void onAppointmentCreated(AppointmentCreated event) {
    String payload =
        """
                {"eventId":"%s","tenantId":"%s","appointmentId":"%s","customerId":"%s","artistId":"%s","serviceId":"%s","startsAt":"%s","endsAt":"%s","timestamp":"%s"}"""
            .formatted(
                event.eventId(),
                event.tenantId(),
                event.appointmentId(),
                event.customerId(),
                event.artistId(),
                event.serviceId(),
                event.startsAt(),
                event.endsAt(),
                event.timestamp());
    broadcast(DashboardSseEvent.appointmentCreated(payload));
  }

  @ApplicationModuleListener
  public void onAppointmentCancelled(AppointmentCancelled event) {
    String payload =
        """
                {"eventId":"%s","tenantId":"%s","appointmentId":"%s","timestamp":"%s"}"""
            .formatted(event.eventId(), event.tenantId(), event.appointmentId(), event.timestamp());
    broadcast(DashboardSseEvent.appointmentCancelled(payload));
  }

  @EventListener
  public void onNotificationDelivered(NotificationDelivered event) {
    broadcastNotification(event.message());
  }

  public void subscribe(SseEmitter emitter) {
    if (emitters.size() >= MAX_SUBSCRIBERS) {
      emitter.completeWithError(new IllegalStateException("Max subscribers reached"));
      log.warn("Subscription rejected: max subscribers ({}) reached", MAX_SUBSCRIBERS);
      return;
    }
    emitter.onCompletion(() -> removeEmitter(emitter, "completed"));
    emitter.onTimeout(() -> removeEmitter(emitter, "timed out"));
    emitter.onError(e -> removeEmitter(emitter, "error: " + e.getMessage()));
    emitters.add(emitter);
    log.info("Dashboard subscriber added. Total: {}", emitters.size());
  }

  public void broadcast(DashboardSseEvent event) {
    log.debug(
        "Broadcasting dashboard event: type={}, subscribers={}", event.type(), emitters.size());
    for (SseEmitter emitter : emitters) {
      try {
        emitter.send(SseEmitter.event().name(event.type()).data(event.payload()));
      } catch (IOException e) {
        removeEmitter(emitter, "send error: " + e.getMessage());
      }
    }
  }

  public void broadcastNotification(String message) {
    String payload = "{\"message\":\"%s\"}".formatted(escapeJson(message));
    broadcast(DashboardSseEvent.notification(payload));
  }

  private void removeEmitter(SseEmitter emitter, String reason) {
    emitters.remove(emitter);
    log.info("Dashboard subscriber removed: {}. Remaining: {}", reason, emitters.size());
  }

  private static String escapeJson(String value) {
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }
}
