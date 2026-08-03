package com.emme.shared.web.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.Test;

class SupportedLocaleTest {

  @Test
  void selectsSpanishMexicoFromAcceptLanguageHeader() {
    assertThat(SupportedLocale.fromHeader("es-MX,es;q=0.9,en;q=0.8"))
        .isEqualTo(Locale.forLanguageTag("es-MX"));
  }

  @Test
  void fallsBackToEnglishForMissingUnsupportedOrMalformedHeaders() {
    assertThat(SupportedLocale.fromHeader(null)).isEqualTo(Locale.US);
    assertThat(SupportedLocale.fromHeader("fr-FR")).isEqualTo(Locale.US);
    assertThat(SupportedLocale.fromHeader("-")).isEqualTo(Locale.US);
  }
}
