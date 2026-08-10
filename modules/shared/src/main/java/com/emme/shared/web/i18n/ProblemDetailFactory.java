package com.emme.shared.web.i18n;

import com.emme.kernel.tracing.CorrelationId;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/** Creates stable, localized RFC 9457 problem details for shared HTTP failures. */
public final class ProblemDetailFactory {

  private final MessageResolver messages;

  public ProblemDetailFactory(MessageResolver messages) {
    this.messages = Objects.requireNonNull(messages, "messages must not be null");
  }

  public ProblemDetail create(
      HttpStatus status, String code, String acceptLanguage, Object... arguments) {
    var problem = ProblemDetail.forStatus(status);
    problem.setTitle(code);
    problem.setDetail(
        messages.resolve("error." + code, SupportedLocale.fromHeader(acceptLanguage), arguments));
    problem.setProperty("code", code);

    var requestId = CorrelationId.get();
    if (requestId != null && !requestId.isBlank()) {
      problem.setProperty("requestId", requestId);
    }
    return problem;
  }
}
