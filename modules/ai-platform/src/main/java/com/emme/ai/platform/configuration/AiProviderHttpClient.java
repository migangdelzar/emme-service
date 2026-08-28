package com.emme.ai.platform.configuration;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/** Transport boundary owned by the AI platform, keeping OkHttp out of provider contracts. */
public final class AiProviderHttpClient {

  private final OkHttpClient delegate;

  public AiProviderHttpClient(OkHttpClient delegate) {
    this.delegate = delegate;
  }

  public Call newCall(Request request) {
    return delegate.newCall(request);
  }
}
