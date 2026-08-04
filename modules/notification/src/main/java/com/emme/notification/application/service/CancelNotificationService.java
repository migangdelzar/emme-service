package com.emme.notification.application.service;

import com.emme.notification.api.command.CancelNotificationCommand;
import com.emme.notification.api.result.NotificationDetails;
import com.emme.notification.api.usecase.CancelNotificationUseCase;
import com.emme.notification.application.mapper.NotificationApplicationMapper;
import com.emme.notification.application.port.out.NotificationRepository;
import com.emme.notification.domain.model.Notification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CancelNotificationService implements CancelNotificationUseCase {
  private final NotificationRepository repository;

  public CancelNotificationService(NotificationRepository repository) {
    this.repository = repository;
  }

  @Override
  public NotificationDetails cancel(CancelNotificationCommand command) {
    Notification notification =
        NotificationServiceSupport.load(repository, command.tenantId(), command.notificationId());
    notification.markCancelled();
    return NotificationApplicationMapper.toResult(repository.save(notification));
  }
}
