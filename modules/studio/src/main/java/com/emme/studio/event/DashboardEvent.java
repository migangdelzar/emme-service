package com.emme.studio.event;

public record DashboardEvent(String type, String payload) {
  public static DashboardEvent appointmentCreated(String payload) {
    return new DashboardEvent("appointment_created", payload);
  }

  public static DashboardEvent appointmentCancelled(String payload) {
    return new DashboardEvent("appointment_cancelled", payload);
  }

  public static DashboardEvent notification(String payload) {
    return new DashboardEvent("notification", payload);
  }
}
