package com.emme.payment.adapter.in.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.payment.api.command.ProcessPaymentCallbackCommand;
import com.emme.payment.api.result.PaymentInfo;
import com.emme.payment.api.usecase.ProcessPaymentCallbackUseCase;
import com.emme.payment.configuration.PaymentProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

class MercadoPagoWebhookControllerTest {

  private static final String SECRET = "webhook-secret";
  private static final UUID TENANT_ID = UUID.randomUUID();

  @Test
  void rejectsMalformedPayloadBeforeCallingTheUseCase() {
    RecordingCallbackUseCase useCase = new RecordingCallbackUseCase();
    MercadoPagoWebhookController controller = controller(useCase);
    MockHttpServletRequest request = request("not-json", "request-1", "payment-1");
    sign(request, "payment-1");

    var response = controller.handleCallback(request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(useCase.command).isNull();
  }

  @Test
  void refusesToProcessWhenWebhookSecretIsMissing() {
    RecordingCallbackUseCase useCase = new RecordingCallbackUseCase();
    PaymentProperties properties =
        new PaymentProperties(
            "mercadopago",
            new PaymentProperties.MercadoPagoConfig("access", "public", ""),
            null,
            null,
            null);
    MercadoPagoWebhookController controller =
        new MercadoPagoWebhookController(
            useCase, properties, new ObjectMapper(), new MercadoPagoWebhookSignatureVerifier());

    var response = controller.handleCallback(request("{}", "request-1", "payment-1"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(useCase.command).isNull();
  }

  @Test
  void delegatesOnlyAuthenticatedTenantScopedCallbacks() {
    RecordingCallbackUseCase useCase = new RecordingCallbackUseCase();
    MercadoPagoWebhookController controller = controller(useCase);
    MockHttpServletRequest request =
        request("{\"type\":\"payment\",\"data\":{\"id\":\"payment-1\"}}", "request-1", "payment-1");
    sign(request, "payment-1");

    var response = controller.handleCallback(request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(useCase.command.tenantId()).isEqualTo(TENANT_ID);
    assertThat(useCase.command.provider()).isEqualTo("mercadopago");
    assertThat(useCase.command.eventId()).isEqualTo("request-1");
    assertThat(useCase.command.payload()).containsEntry("id", "payment-1");
  }

  private MercadoPagoWebhookController controller(RecordingCallbackUseCase useCase) {
    PaymentProperties properties =
        new PaymentProperties(
            "mercadopago",
            new PaymentProperties.MercadoPagoConfig("access", "public", SECRET),
            null,
            null,
            null);
    return new MercadoPagoWebhookController(
        useCase, properties, new ObjectMapper(), new MercadoPagoWebhookSignatureVerifier());
  }

  private MockHttpServletRequest request(String body, String requestId, String dataId) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setContent(body.getBytes(StandardCharsets.UTF_8));
    request.addHeader("x-request-id", requestId);
    request.addParameter("data.id", dataId);
    request.addHeader("X-Tenant-ID", TENANT_ID.toString());
    return request;
  }

  private void sign(MockHttpServletRequest request, String dataId) {
    String manifest = "id:" + dataId + ";request-id:request-1;ts:1704908010;";
    request.addHeader("x-signature", "ts=1704908010,v1=" + hmacHex(manifest, SECRET));
  }

  private static String hmacHex(String message, String secret) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      StringBuilder result = new StringBuilder();
      for (byte value : mac.doFinal(message.getBytes(StandardCharsets.UTF_8))) {
        result.append(String.format("%02x", value));
      }
      return result.toString();
    } catch (GeneralSecurityException exception) {
      throw new AssertionError(exception);
    }
  }

  private static final class RecordingCallbackUseCase implements ProcessPaymentCallbackUseCase {
    private ProcessPaymentCallbackCommand command;

    @Override
    public PaymentInfo process(ProcessPaymentCallbackCommand command) {
      this.command = command;
      return new PaymentInfo(
          UUID.randomUUID(),
          command.tenantId(),
          "payment-1",
          BigDecimal.ZERO,
          "MXN",
          com.emme.payment.domain.model.PaymentStatus.PENDING,
          java.time.Instant.now());
    }
  }
}
