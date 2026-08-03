package com.emme.calendar.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Google OAuth configuration — binds to app.google.oauth.* in application.yml.
 *
 * <p>Example: app: google: oauth: client-id: xxx client-secret: xxx redirect-uri:
 * http://localhost:8080/api/google/oauth/callback encryption-key: 32-chars-base64
 */
@ConfigurationProperties(prefix = "app.google.oauth")
public record GoogleOAuthConfig(
    String clientId, String clientSecret, String redirectUri, String encryptionKey) {}
