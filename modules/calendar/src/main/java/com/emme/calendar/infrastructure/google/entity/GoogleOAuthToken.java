package com.emme.calendar.infrastructure.google.entity;

import com.emme.shared.TenantOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "google_oauth_token")
public class GoogleOAuthToken extends TenantOwnedEntity {

  @Column(name = "user_id", nullable = false)
  private String userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "persona_type", nullable = false, length = 20)
  private PersonaType personaType;

  @Column(name = "access_token", columnDefinition = "TEXT", nullable = false)
  private String accessToken;

  @Column(name = "refresh_token", columnDefinition = "TEXT")
  private String refreshToken;

  @Column(name = "scopes")
  private String scopes;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "provider_email")
  private String providerEmail;

  protected GoogleOAuthToken() {}

  public GoogleOAuthToken(
      UUID tenantId,
      String userId,
      PersonaType personaType,
      String accessToken,
      String refreshToken,
      String scopes,
      Instant expiresAt) {
    super(tenantId);
    this.userId = Objects.requireNonNull(userId, "userId must not be null");
    this.personaType = Objects.requireNonNull(personaType, "personaType must not be null");
    this.accessToken = Objects.requireNonNull(accessToken, "accessToken must not be null");
    this.refreshToken = refreshToken;
    this.scopes = scopes;
    this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
  }

  public String getUserId() {
    return userId;
  }

  public PersonaType getPersonaType() {
    return personaType;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = Objects.requireNonNull(accessToken, "accessToken must not be null");
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }

  public String getScopes() {
    return scopes;
  }

  public void setScopes(String scopes) {
    this.scopes = scopes;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
  }

  public String getProviderEmail() {
    return providerEmail;
  }

  public void setProviderEmail(String providerEmail) {
    this.providerEmail = providerEmail;
  }

  /** Returns true if the access token has already expired. */
  public boolean isExpired() {
    return Instant.now().isAfter(expiresAt);
  }
}
