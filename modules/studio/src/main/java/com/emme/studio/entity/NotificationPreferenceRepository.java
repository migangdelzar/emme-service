package com.emme.studio.entity;

import com.emme.kernel.type.NotificationChannel;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationPreferenceRepository
    extends JpaRepository<NotificationPreference, UUID> {
  List<NotificationPreference> findByTenantId(UUID tenantId);

  Optional<NotificationPreference> findByTenantIdAndChannel(
      UUID tenantId, NotificationChannel channel);
}
