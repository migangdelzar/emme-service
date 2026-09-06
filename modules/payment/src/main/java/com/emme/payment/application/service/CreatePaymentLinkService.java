package com.emme.payment.application.service;

import com.emme.ai.contracts.payment.PaymentLink;
import com.emme.payment.api.command.CreatePaymentLinkCommand;
import com.emme.payment.api.usecase.CreatePaymentLinkUseCase;
import com.emme.payment.application.port.out.PaymentLinkRepository;
import com.emme.payment.application.port.out.PaymentLinkSourceRepository;
import com.emme.payment.application.port.out.PaymentProvider;
import com.emme.payment.application.port.out.PaymentProviderException;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Creates an idempotent checkout link from trusted persisted payment facts. */
public final class CreatePaymentLinkService implements CreatePaymentLinkUseCase {

  private final PaymentLinkRepository links;
  private final PaymentLinkSourceRepository sources;
  private final PaymentProvider provider;

  public CreatePaymentLinkService(
      PaymentLinkRepository links, PaymentLinkSourceRepository sources, PaymentProvider provider) {
    this.links = Objects.requireNonNull(links, "links must not be null");
    this.sources = Objects.requireNonNull(sources, "sources must not be null");
    this.provider = Objects.requireNonNull(provider, "provider must not be null");
  }

  @Override
  public PaymentLink create(CreatePaymentLinkCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    var existing = links.findByIdempotencyKey(command.idempotencyKey());
    if (existing.isPresent()) {
      return existing.orElseThrow();
    }

    PaymentLinkSourceRepository.PaymentLinkSource source =
        sources
            .findByWorkflowIdAndHoldId(command.workflowId(), command.holdId())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Payment link source not found for workflow " + command.workflowId()));
    PaymentProvider.PaymentResult result =
        provider.initiate(
            command.idempotencyKey(), source.amount(), source.currency(), source.description());
    String checkoutUrl = checkoutUrl(result);
    return links.save(
        new PaymentLink(
            UUID.randomUUID(),
            command.workflowId(),
            provider.name(),
            checkoutUrl,
            source.expiresAt()),
        command.idempotencyKey());
  }

  private static String checkoutUrl(PaymentProvider.PaymentResult result) {
    if (result == null || result.metadata() == null) {
      throw new PaymentProviderException("Provider did not return checkout metadata");
    }
    Map<String, String> metadata = result.metadata();
    String checkoutUrl = firstNonBlank(metadata, "checkout_url", "init_point", "approval_url");
    if (checkoutUrl == null) {
      throw new PaymentProviderException("Provider did not return a checkout URL");
    }
    return checkoutUrl;
  }

  private static String firstNonBlank(Map<String, String> metadata, String... keys) {
    for (String key : keys) {
      String value = metadata.get(key);
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return null;
  }
}
