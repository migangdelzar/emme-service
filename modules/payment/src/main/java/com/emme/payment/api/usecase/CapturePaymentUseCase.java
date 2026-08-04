package com.emme.payment.api.usecase;

import com.emme.payment.api.command.CapturePaymentCommand;
import com.emme.payment.api.result.PaymentDetails;

public interface CapturePaymentUseCase {
  PaymentDetails capture(CapturePaymentCommand command);
}
