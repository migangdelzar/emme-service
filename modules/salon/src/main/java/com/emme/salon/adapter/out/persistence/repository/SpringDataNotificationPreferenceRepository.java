package com.emme.salon.adapter.out.persistence.repository;

import com.emme.kernel.type.NotificationChannel;
import com.emme.salon.adapter.out.persistence.entity.NotificationPreferenceEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataNotificationPreferenceRepository
    extends JpaRepository<NotificationPreferenceEntity, UUID> {
  Optional<NotificationPreferenceEntity> findByTenantIdAndChannel(
      UUID tenantId, NotificationChannel channel);
}
