package com.emme.calendar.configuration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Google OAuth configuration — binds to app.google.oauth.* in application.yml.
 *
 * <p>Example: app: google: oauth: client-id: xxx client-secret: xxx redirect-uri:
 * http://localhost:8080/api/google/oauth/callback encryption-key: 32-chars-base64
 */
@ConfigurationProperties(prefix = "app.google.oauth")
@Validated
public record GoogleOAuthProperties(
    String clientId,
    String clientSecret,
    String redirectUri,
    @NotBlank @Size(min = 32, max = 32) String encryptionKey) {}
