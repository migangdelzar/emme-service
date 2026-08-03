package com.emme.notification.configuration;

import java.util.Objects;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/** Capability-owned HTTP boundary for notification provider adapters. */
public final class NotificationHttpClient {
  private final OkHttpClient delegate;

  public NotificationHttpClient(OkHttpClient delegate) {
    this.delegate = Objects.requireNonNull(delegate, "delegate");
  }

  public Call newCall(Request request) {
    return delegate.newCall(request);
  }
}
