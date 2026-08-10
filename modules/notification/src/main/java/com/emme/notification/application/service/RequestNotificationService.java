package com.emme.notification.application.service;

import com.emme.notification.api.command.RequestNotificationCommand;
import com.emme.notification.api.result.NotificationDetails;
import com.emme.notification.api.usecase.RequestNotificationUseCase;
import com.emme.notification.application.mapper.NotificationApplicationMapper;
import com.emme.notification.application.port.out.NotificationRepository;
import com.emme.notification.domain.model.Notification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RequestNotificationService implements RequestNotificationUseCase {
  private final NotificationRepository repository;

  public RequestNotificationService(NotificationRepository repository) {
    this.repository = repository;
  }

  @Override
  public NotificationDetails request(RequestNotificationCommand command) {
    return NotificationApplicationMapper.toResult(
        repository.save(
            new Notification(
                command.tenantId(), command.channel(), command.recipient(), command.message())));
  }
}
