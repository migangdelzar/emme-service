package com.emme.shared.web.i18n;

import java.util.Locale;
import java.util.Objects;
import org.springframework.context.MessageSource;

/** Resolves public HTTP messages without exposing Spring message-source details to callers. */
public final class MessageResolver {

  private final MessageSource messageSource;

  public MessageResolver(MessageSource messageSource) {
    this.messageSource = Objects.requireNonNull(messageSource, "messageSource must not be null");
  }

  public String resolve(String code, Locale locale, Object... arguments) {
    return messageSource.getMessage(code, arguments, code, locale);
  }
}
