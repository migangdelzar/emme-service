package com.emme.client;

/** Shared JSON parsing utilities for E2E tests. */
public final class E2eJson {

  private E2eJson() {}

  /** Extract a string field value from JSON. */
  public static String stringField(String json, String field) {
    var prefix = "\"" + field + "\":\"";
    int start = json.indexOf(prefix);
    if (start < 0) return null;
    start += prefix.length();
    int end = json.indexOf("\"", start);
    return json.substring(start, end);
  }

  /** Extract a non-string field value from JSON (numbers, booleans, null). */
  public static String rawField(String json, String field) {
    var prefix = "\"" + field + "\":";
    int start = json.indexOf(prefix);
    if (start < 0) return null;
    start += prefix.length();
    int end = json.indexOf(",", start);
    if (end < 0) end = json.indexOf("}", start);
    if (end < 0) return null;
    return json.substring(start, end).trim();
  }

  /** Convenience: extract a string field value. Delegates to stringField. */
  public static String extract(String json, String field) {
    return stringField(json, field);
  }
}
