package com.emme.payment.api.usecase;

import com.emme.payment.api.command.CapturePaymentCommand;
import com.emme.payment.api.result.PaymentInfo;

public interface CapturePaymentUseCase {
  PaymentInfo capture(CapturePaymentCommand command);
}
