package com.emme.payment.adapter.in.web.mapper;

import com.emme.payment.adapter.in.web.request.InitiatePaymentRequest;
import com.emme.payment.api.command.InitiatePaymentCommand;
import java.util.UUID;

public final class PaymentWebMapper {
  private PaymentWebMapper() {}

  public static InitiatePaymentCommand toCommand(UUID tenantId, InitiatePaymentRequest request) {
    return new InitiatePaymentCommand(
        tenantId, request.providerReference(), request.amount(), request.currency());
  }
}
