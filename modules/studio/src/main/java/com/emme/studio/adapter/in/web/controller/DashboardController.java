package com.emme.studio.adapter.in.web.controller;

import com.emme.studio.adapter.in.web.sse.DashboardBroadcaster;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard")
public class DashboardController {

  private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

  private final DashboardBroadcaster broadcaster;

  public DashboardController(DashboardBroadcaster broadcaster) {
    this.broadcaster = broadcaster;
  }

  @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  @Operation(summary = "Subscribe to real-time dashboard events via SSE")
  public SseEmitter stream() {
    SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
    broadcaster.subscribe(emitter);
    try {
      emitter.send(SseEmitter.event().name("connected").data("{\"status\":\"connected\"}"));
      log.info("Dashboard SSE stream connected");
    } catch (Exception e) {
      emitter.completeWithError(e);
      log.warn("Failed to send connected event to dashboard SSE stream", e);
    }
    return emitter;
  }
}
