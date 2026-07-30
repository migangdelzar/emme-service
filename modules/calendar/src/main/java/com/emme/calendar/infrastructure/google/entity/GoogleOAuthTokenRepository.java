package com.emme.calendar.infrastructure.google.entity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface GoogleOAuthTokenRepository extends JpaRepository<GoogleOAuthToken, UUID> {

  Optional<GoogleOAuthToken> findByTenantIdAndUserIdAndPersonaType(
      UUID tenantId, String userId, PersonaType personaType);

  List<GoogleOAuthToken> findByTenantId(UUID tenantId);

  /** Delete tokens whose expiry hasn't been updated since the cutoff date. */
  @Modifying
  @Query("DELETE FROM GoogleOAuthToken t WHERE t.expiresAt < :cutoff")
  long purgeExpiredTokens(Instant cutoff);
}
