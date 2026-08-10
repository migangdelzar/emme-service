package com.emme.shared.web.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;

class MessageResolverTest {

  @Test
  void resolvesLocalizedMessageWithArguments() {
    var source = new StaticMessageSource();
    source.addMessage("error.TENANT_NOT_FOUND", Locale.US, "Tenant {0} was not found");
    var resolver = new MessageResolver(source);

    assertThat(resolver.resolve("error.TENANT_NOT_FOUND", Locale.US, "tenant-123"))
        .isEqualTo("Tenant tenant-123 was not found");
  }

  @Test
  void returnsTheMessageCodeWhenNoTranslationExists() {
    var resolver = new MessageResolver(new StaticMessageSource());

    assertThat(resolver.resolve("error.UNKNOWN", Locale.US)).isEqualTo("error.UNKNOWN");
  }
}
