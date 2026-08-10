package com.emme.payment.api.usecase;

import com.emme.payment.api.command.InitiatePaymentCommand;
import com.emme.payment.api.result.PaymentDetails;

public interface InitiatePaymentUseCase {
  PaymentDetails initiate(InitiatePaymentCommand command);
}
