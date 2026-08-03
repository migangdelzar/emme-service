package com.emme.notification.api.usecase;

import com.emme.notification.api.query.ListNotificationsQuery;
import com.emme.notification.api.result.NotificationInfo;
import java.util.List;

public interface ListNotificationsUseCase {
  List<NotificationInfo> list(ListNotificationsQuery query);
}
