package com.emme.payment.configuration;

import com.emme.payment.adapter.out.provider.PaymentProperties;
import com.emme.payment.adapter.out.provider.PaymentProvider;
import com.emme.payment.adapter.out.provider.PaymentProviderException;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Selects the active {@link PaymentProvider} based on {@code app.payment.provider} property. Falls
 * back to mock provider if the configured one is not available.
 */
@Configuration
class PaymentProviderConfig {

  @Bean
  @Primary
  PaymentProvider paymentProvider(List<PaymentProvider> providers, PaymentProperties props) {
    return providers.stream()
        .filter(p -> p.name().equalsIgnoreCase(props.provider()))
        .findFirst()
        .orElseGet(
            () ->
                providers.stream()
                    .filter(p -> "mock".equals(p.name()))
                    .findFirst()
                    .orElseThrow(
                        () ->
                            new PaymentProviderException(
                                "No PaymentProvider found for '"
                                    + props.provider()
                                    + "' and no mock fallback available")));
  }
}
