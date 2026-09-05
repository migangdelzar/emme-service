package com.emme.notification.adapter.out.persistence.repository;

import com.emme.notification.adapter.out.persistence.entity.NotificationEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataNotificationRepository extends JpaRepository<NotificationEntity, UUID> {}
