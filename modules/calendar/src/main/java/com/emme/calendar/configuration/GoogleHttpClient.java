package com.emme.calendar.configuration;

import java.util.Objects;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/** Capability-owned HTTP boundary for Google Calendar and Sheets adapters. */
public final class GoogleHttpClient {
  private final OkHttpClient delegate;

  public GoogleHttpClient(OkHttpClient delegate) {
    this.delegate = Objects.requireNonNull(delegate, "delegate");
  }

  public Call newCall(Request request) {
    return delegate.newCall(request);
  }
}
