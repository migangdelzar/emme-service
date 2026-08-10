package com.emme.assistant.ai.configuration;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/** Capability-owned HTTP client boundary for AI and Assistant external calls. */
public final class AiHttpClient {

  private final OkHttpClient delegate;

  public AiHttpClient(OkHttpClient delegate) {
    this.delegate = delegate;
  }

  public Call newCall(Request request) {
    return delegate.newCall(request);
  }
}
