package com.emme.calendar.adapter.out.persistence.repository;

import com.emme.calendar.adapter.out.google.model.PersonaType;
import com.emme.calendar.adapter.out.persistence.entity.GoogleOAuthTokenEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataGoogleOAuthTokenRepository
    extends JpaRepository<GoogleOAuthTokenEntity, UUID> {

  Optional<GoogleOAuthTokenEntity> findByTenantIdAndUserIdAndPersonaType(
      UUID tenantId, String userId, PersonaType personaType);

  List<GoogleOAuthTokenEntity> findByTenantId(UUID tenantId);

  /** Delete tokens whose expiry hasn't been updated since the cutoff date. */
  @Modifying
  @Query("DELETE FROM GoogleOAuthTokenEntity t WHERE t.expiresAt < :cutoff")
  long purgeExpiredTokens(Instant cutoff);
}
