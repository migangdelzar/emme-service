package com.emme.payment.api.usecase;

import com.emme.payment.api.command.AuthorizePaymentCommand;
import com.emme.payment.api.result.PaymentDetails;

public interface AuthorizePaymentUseCase {
  PaymentDetails authorize(AuthorizePaymentCommand command);
}
