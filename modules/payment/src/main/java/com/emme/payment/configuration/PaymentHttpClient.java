package com.emme.payment.configuration;

import java.util.Objects;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/** Capability-owned HTTP boundary for payment provider adapters. */
public final class PaymentHttpClient {
  private final OkHttpClient delegate;

  public PaymentHttpClient(OkHttpClient delegate) {
    this.delegate = Objects.requireNonNull(delegate, "delegate");
  }

  public Call newCall(Request request) {
    return delegate.newCall(request);
  }
}
