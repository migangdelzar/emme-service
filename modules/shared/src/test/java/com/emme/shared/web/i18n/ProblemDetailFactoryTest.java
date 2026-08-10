package com.emme.shared.web.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.kernel.tracing.CorrelationId;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.HttpStatus;

class ProblemDetailFactoryTest {

  @AfterEach
  void clearCorrelationId() {
    CorrelationId.clear();
  }

  @Test
  void createsLocalizedProblemDetailsWithStableCodeAndRequestId() {
    var source = new StaticMessageSource();
    source.addMessage("error.TENANT_NOT_FOUND", Locale.US, "Tenant {0} was not found");
    var factory = new ProblemDetailFactory(new MessageResolver(source));
    CorrelationId.set("corr-123");

    var problem = factory.create(HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND", "en-US", "tenant-123");

    assertThat(problem.getStatus()).isEqualTo(404);
    assertThat(problem.getDetail()).isEqualTo("Tenant tenant-123 was not found");
    assertThat(problem.getProperties())
        .containsEntry("code", "TENANT_NOT_FOUND")
        .containsEntry("requestId", "corr-123");
  }
}
