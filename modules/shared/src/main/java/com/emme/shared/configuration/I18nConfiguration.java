package com.emme.shared.configuration;

import com.emme.shared.web.i18n.MessageResolver;
import com.emme.shared.web.i18n.ProblemDetailFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

/** Configures the shared, deterministic HTTP message source. */
@Configuration(proxyBeanMethods = false)
public class I18nConfiguration {

  @Bean
  MessageSource messageSource() {
    var source = new ResourceBundleMessageSource();
    source.setBasenames("i18n/messages");
    source.setDefaultEncoding("UTF-8");
    source.setFallbackToSystemLocale(false);
    return source;
  }

  @Bean
  MessageResolver messageResolver(MessageSource messageSource) {
    return new MessageResolver(messageSource);
  }

  @Bean
  ProblemDetailFactory problemDetailFactory(MessageResolver messageResolver) {
    return new ProblemDetailFactory(messageResolver);
  }
}
