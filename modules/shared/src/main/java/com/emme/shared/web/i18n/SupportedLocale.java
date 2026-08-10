package com.emme.shared.web.i18n;

import java.util.List;
import java.util.Locale;

/** Supported HTTP locales with deterministic English fallback behavior. */
public enum SupportedLocale {
  EN_US(Locale.US),
  ES_MX(Locale.forLanguageTag("es-MX"));

  private static final List<Locale> SUPPORTED = List.of(EN_US.locale, ES_MX.locale);

  private final Locale locale;

  SupportedLocale(Locale locale) {
    this.locale = locale;
  }

  public Locale locale() {
    return locale;
  }

  public static Locale fromHeader(String acceptLanguage) {
    if (acceptLanguage == null || acceptLanguage.isBlank()) {
      return EN_US.locale;
    }

    try {
      var ranges = Locale.LanguageRange.parse(acceptLanguage);
      var match = Locale.lookup(ranges, SUPPORTED);
      return match == null ? EN_US.locale : match;
    } catch (IllegalArgumentException | IndexOutOfBoundsException exception) {
      return EN_US.locale;
    }
  }
}
