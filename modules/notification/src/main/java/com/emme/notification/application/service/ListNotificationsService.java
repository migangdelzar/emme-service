package com.emme.notification.application.service;

import com.emme.notification.api.query.ListNotificationsQuery;
import com.emme.notification.api.result.NotificationDetails;
import com.emme.notification.api.usecase.ListNotificationsUseCase;
import com.emme.notification.application.mapper.NotificationApplicationMapper;
import com.emme.notification.application.port.out.NotificationRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ListNotificationsService implements ListNotificationsUseCase {
  private final NotificationRepository repository;

  public ListNotificationsService(NotificationRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<NotificationDetails> list(ListNotificationsQuery query) {
    return repository.findByTenantId(query.tenantId()).stream()
        .map(NotificationApplicationMapper::toResult)
        .toList();
  }
}
