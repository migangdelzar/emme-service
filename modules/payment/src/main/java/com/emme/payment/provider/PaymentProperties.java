package com.emme.payment.provider;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Payment configuration — binds to app.payment.* in application.yml.
 *
 * <p>Example: app: payment: provider: mercadopago mercadopago: access-token: APP_USR-...
 * public-key: APP_USR-... webhook-secret: ...
 */
@ConfigurationProperties("app.payment")
public record PaymentProperties(
    String provider,
    MercadoPagoConfig mercadopago,
    PayPalConfig paypal,
    ConektaConfig conekta,
    StripeConfig stripe) {
  public PaymentProperties {
    if (provider == null || provider.isBlank()) provider = "mock";
  }

  public record MercadoPagoConfig(String accessToken, String publicKey, String webhookSecret) {}

  public record PayPalConfig(String clientId, String clientSecret, String webhookId) {}

  public record ConektaConfig(String privateKey, String webhookSecret) {}

  public record StripeConfig(String secretKey, String webhookSecret) {}
}
