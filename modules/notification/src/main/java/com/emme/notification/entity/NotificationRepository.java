package com.emme.notification.entity;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

  List<Notification> findByTenantId(UUID tenantId);

  List<Notification> findByTenantIdAndStatus(UUID tenantId, NotificationStatus status);
}
