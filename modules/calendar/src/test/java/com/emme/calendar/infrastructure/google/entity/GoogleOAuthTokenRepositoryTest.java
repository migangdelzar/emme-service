package com.emme.calendar.infrastructure.google.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.testing.BaseRepositoryTest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(
    properties = {
      "app.google.oauth.client-id=test-client-id",
      "app.google.oauth.client-secret=test-client-secret",
      "app.google.oauth.redirect-uri=http://localhost/callback",
      "app.google.oauth.encryption-key=12345678901234567890123456789012" // 32 bytes for AES-256
    })
class GoogleOAuthTokenRepositoryTest extends BaseRepositoryTest {

  @Autowired private GoogleOAuthTokenRepository tokenRepo;

  private static final UUID TENANT_ID = UUID.randomUUID();
  private static final String USER_ID = "auth0|test-staff";

  @Test
  void shouldSaveAndFindTokenByTenantUserAndPersona() {
    Instant expires = Instant.now().plus(1, ChronoUnit.HOURS);
    GoogleOAuthToken token =
        new GoogleOAuthToken(
            TENANT_ID,
            USER_ID,
            PersonaType.STAFF,
            "encrypted-access-token",
            "encrypted-refresh-token",
            "calendar spreadsheets",
            expires);

    GoogleOAuthToken saved = tokenRepo.save(token);
    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getTenantId()).isEqualTo(TENANT_ID);
    assertThat(saved.getUserId()).isEqualTo(USER_ID);
    assertThat(saved.getPersonaType()).isEqualTo(PersonaType.STAFF);

    Optional<GoogleOAuthToken> found =
        tokenRepo.findByTenantIdAndUserIdAndPersonaType(TENANT_ID, USER_ID, PersonaType.STAFF);
    assertThat(found).isPresent();
    assertThat(found.get().getAccessToken()).isEqualTo("encrypted-access-token");
    assertThat(found.get().getRefreshToken()).isEqualTo("encrypted-refresh-token");
    assertThat(found.get().getScopes()).isEqualTo("calendar spreadsheets");
  }

  @Test
  void shouldFindAllTokensForTenant() {
    Instant expires = Instant.now().plus(1, ChronoUnit.HOURS);
    tokenRepo.save(
        new GoogleOAuthToken(
            TENANT_ID, "user-1", PersonaType.STAFF, "at1", "rt1", "scopes", expires));
    tokenRepo.save(
        new GoogleOAuthToken(
            TENANT_ID, "user-2", PersonaType.CLIENT, "at2", "rt2", "scopes", expires));

    List<GoogleOAuthToken> tokens = tokenRepo.findByTenantId(TENANT_ID);
    assertThat(tokens).hasSize(2);
  }

  @Test
  void shouldReturnEmptyWhenNoTokenForUserAndPersona() {
    Optional<GoogleOAuthToken> found =
        tokenRepo.findByTenantIdAndUserIdAndPersonaType(
            TENANT_ID, "nonexistent", PersonaType.STAFF);
    assertThat(found).isEmpty();
  }

  @Test
  void shouldUpdateExistingToken() {
    Instant expires = Instant.now().plus(1, ChronoUnit.HOURS);
    GoogleOAuthToken token =
        new GoogleOAuthToken(
            TENANT_ID, USER_ID, PersonaType.STAFF, "original-at", "original-rt", "scopes", expires);
    GoogleOAuthToken saved = tokenRepo.save(token);

    saved.setAccessToken("updated-at");
    tokenRepo.save(saved);

    Optional<GoogleOAuthToken> found =
        tokenRepo.findByTenantIdAndUserIdAndPersonaType(TENANT_ID, USER_ID, PersonaType.STAFF);
    assertThat(found).isPresent();
    assertThat(found.get().getAccessToken()).isEqualTo("updated-at");
  }

  @Test
  void shouldDistinguishTokenByPersonaType() {
    Instant expires = Instant.now().plus(1, ChronoUnit.HOURS);
    tokenRepo.save(
        new GoogleOAuthToken(
            TENANT_ID, USER_ID, PersonaType.STAFF, "staff-at", "staff-rt", "sc", expires));
    tokenRepo.save(
        new GoogleOAuthToken(
            TENANT_ID, USER_ID, PersonaType.CLIENT, "client-at", "client-rt", "sc", expires));

    Optional<GoogleOAuthToken> staff =
        tokenRepo.findByTenantIdAndUserIdAndPersonaType(TENANT_ID, USER_ID, PersonaType.STAFF);
    Optional<GoogleOAuthToken> client =
        tokenRepo.findByTenantIdAndUserIdAndPersonaType(TENANT_ID, USER_ID, PersonaType.CLIENT);

    assertThat(staff).isPresent();
    assertThat(client).isPresent();
    assertThat(staff.get().getAccessToken()).isEqualTo("staff-at");
    assertThat(client.get().getAccessToken()).isEqualTo("client-at");
  }

  @Test
  void shouldDetectExpiredToken() {
    Instant expiredTime = Instant.now().minus(1, ChronoUnit.HOURS);
    GoogleOAuthToken token =
        new GoogleOAuthToken(
            TENANT_ID, USER_ID, PersonaType.STAFF, "at", "rt", "scopes", expiredTime);
    tokenRepo.save(token);

    Optional<GoogleOAuthToken> found =
        tokenRepo.findByTenantIdAndUserIdAndPersonaType(TENANT_ID, USER_ID, PersonaType.STAFF);
    assertThat(found).isPresent();
    assertThat(found.get().isExpired()).isTrue();
  }
}
