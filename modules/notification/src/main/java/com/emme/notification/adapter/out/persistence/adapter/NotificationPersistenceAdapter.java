package com.emme.notification.adapter.out.persistence.adapter;

import com.emme.notification.adapter.out.persistence.entity.NotificationEntity;
import com.emme.notification.adapter.out.persistence.mapper.NotificationPersistenceMapper;
import com.emme.notification.adapter.out.persistence.repository.SpringDataNotificationRepository;
import com.emme.notification.application.port.out.NotificationRepository;
import com.emme.notification.domain.model.Notification;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class NotificationPersistenceAdapter implements NotificationRepository {
  private final SpringDataNotificationRepository repository;
  private final NotificationPersistenceMapper mapper;

  public NotificationPersistenceAdapter(
      SpringDataNotificationRepository repository, NotificationPersistenceMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public Optional<Notification> findByTenantIdAndId(UUID tenantId, UUID notificationId) {
    return repository.findByTenantIdAndId(tenantId, notificationId).map(mapper::toDomain);
  }

  @Override
  public List<Notification> findByTenantId(UUID tenantId) {
    return repository.findByTenantId(tenantId).stream().map(mapper::toDomain).toList();
  }

  @Override
  public Notification save(Notification notification) {
    NotificationEntity saved = repository.save(mapper.toEntity(notification));
    return mapper.toDomain(saved);
  }
}
