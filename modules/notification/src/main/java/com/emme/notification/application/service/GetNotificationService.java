package com.emme.notification.application.service;

import com.emme.notification.api.query.GetNotificationQuery;
import com.emme.notification.api.result.NotificationDetails;
import com.emme.notification.api.usecase.GetNotificationUseCase;
import com.emme.notification.application.mapper.NotificationApplicationMapper;
import com.emme.notification.application.port.out.NotificationRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetNotificationService implements GetNotificationUseCase {
  private final NotificationRepository repository;

  public GetNotificationService(NotificationRepository repository) {
    this.repository = repository;
  }

  @Override
  public Optional<NotificationDetails> get(GetNotificationQuery query) {
    return repository
        .findByTenantIdAndId(query.tenantId(), query.notificationId())
        .map(NotificationApplicationMapper::toResult);
  }
}
