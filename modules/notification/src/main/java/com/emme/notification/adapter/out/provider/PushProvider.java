package com.emme.notification.adapter.out.provider;

import java.util.Map;

/**
 * Abstraction for push notification providers (FCM, APNs, Mock). Implementations handle the full
 * send lifecycle — token-based auth, message construction, and delivery to the platform gateway.
 */
public interface PushProvider {

  /** Provider identifier (e.g. "fcm", "apns", "mock") */
  String name();

  /** Send a push notification. Returns the provider-assigned message ID. */
  String send(String deviceToken, String title, String body, Map<String, String> data);

  /** Whether this provider is a mock implementation */
  default boolean isMock() {
    return false;
  }
}
