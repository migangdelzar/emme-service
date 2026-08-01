package com.emme.notification.api.usecase;

import com.emme.notification.api.query.GetNotificationQuery;
import com.emme.notification.api.result.NotificationInfo;
import java.util.Optional;

public interface GetNotificationUseCase {
  Optional<NotificationInfo> get(GetNotificationQuery query);
}
