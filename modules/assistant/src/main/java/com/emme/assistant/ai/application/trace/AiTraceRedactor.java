package com.emme.assistant.ai.application.trace;

import java.util.regex.Pattern;

/** Deterministic defense-in-depth redaction for durable AI telemetry payloads. */
public final class AiTraceRedactor {

  private static final Pattern EMAIL =
      Pattern.compile(
          "(?i)(?<![A-Za-z0-9._%+-])[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}(?![A-Za-z0-9.-])");
  private static final Pattern PHONE =
      Pattern.compile("(?<!\\d)(?:\\+?\\d[\\d ()-]{7,}\\d)(?!\\d)");
  private static final Pattern BEARER = Pattern.compile("(?i)\\bBearer\\s+\\S+");

  public String redact(String value) {
    if (value == null) return null;
    return BEARER
        .matcher(
            PHONE
                .matcher(EMAIL.matcher(value).replaceAll("[REDACTED_EMAIL]"))
                .replaceAll("[REDACTED_PHONE]"))
        .replaceAll("[REDACTED_BEARER]");
  }
}
