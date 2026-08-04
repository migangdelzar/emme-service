package com.emme.notification.api.usecase;

import com.emme.notification.api.query.GetNotificationQuery;
import com.emme.notification.api.result.NotificationDetails;
import java.util.Optional;

public interface GetNotificationUseCase {
  Optional<NotificationDetails> get(GetNotificationQuery query);
}
