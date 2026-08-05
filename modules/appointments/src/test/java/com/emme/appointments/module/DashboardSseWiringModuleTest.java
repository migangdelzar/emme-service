package com.emme.appointments.module;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.emme.notification.api.event.NotificationDelivered;
import com.emme.appointments.adapter.in.web.sse.DashboardBroadcaster;
import com.emme.testing.BaseSpringModuleTest;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Verifies the notifications -> salon event wiring: a {@link NotificationDelivered} published
 * through the application context reaches {@link DashboardBroadcaster} subscribers via its
 * {@code @EventListener}, replacing the former direct cross-module call.
 */
class DashboardSseWiringModuleTest extends BaseSpringModuleTest {

  @Autowired private ApplicationEventPublisher events;
  @Autowired private DashboardBroadcaster broadcaster;

  private Runnable completionCallback;

  @AfterEach
  void unsubscribe() {
    if (completionCallback != null) {
      completionCallback.run();
    }
  }

  @Test
  void notificationDeliveredEventReachesDashboardSubscribers() throws IOException {
    SseEmitter emitter = mock(SseEmitter.class);
    broadcaster.subscribe(emitter);
    ArgumentCaptor<Runnable> onCompletion = ArgumentCaptor.forClass(Runnable.class);
    verify(emitter).onCompletion(onCompletion.capture());
    completionCallback = onCompletion.getValue();

    events.publishEvent(new NotificationDelivered("Your appointment is confirmed"));

    verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
  }
}
