package com.emme.studio.adapter.in.web.sse;

record DashboardSseEvent(String type, String payload) {
  static DashboardSseEvent appointmentCreated(String payload) {
    return new DashboardSseEvent("appointment_created", payload);
  }

  static DashboardSseEvent appointmentCancelled(String payload) {
    return new DashboardSseEvent("appointment_cancelled", payload);
  }

  static DashboardSseEvent notification(String payload) {
    return new DashboardSseEvent("notification", payload);
  }
}
