package com.emme.payment.api.usecase;

import com.emme.ai.contracts.payment.PaymentLink;
import com.emme.payment.api.command.CreatePaymentLinkCommand;

public interface CreatePaymentLinkUseCase {

  PaymentLink create(CreatePaymentLinkCommand command);
}
