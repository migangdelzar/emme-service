package com.emme.payment.api.usecase;

import com.emme.payment.api.command.ProcessPaymentCallbackCommand;
import com.emme.payment.api.result.PaymentDetails;

public interface ProcessPaymentCallbackUseCase {
  PaymentDetails process(ProcessPaymentCallbackCommand command);
}
