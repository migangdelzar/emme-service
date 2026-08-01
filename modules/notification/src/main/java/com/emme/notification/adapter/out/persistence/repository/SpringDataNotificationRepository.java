package com.emme.notification.adapter.out.persistence.repository;

import com.emme.notification.adapter.out.persistence.entity.NotificationEntity;
import com.emme.notification.domain.model.NotificationStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataNotificationRepository extends JpaRepository<NotificationEntity, UUID> {

  List<NotificationEntity> findByTenantId(UUID tenantId);

  Optional<NotificationEntity> findByTenantIdAndId(UUID tenantId, UUID notificationId);

  List<NotificationEntity> findByTenantIdAndStatus(UUID tenantId, NotificationStatus status);
}
