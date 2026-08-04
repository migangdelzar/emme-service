package com.emme.notification.api.usecase;

import com.emme.notification.api.query.ListNotificationsQuery;
import com.emme.notification.api.result.NotificationDetails;
import java.util.List;

public interface ListNotificationsUseCase {
  List<NotificationDetails> list(ListNotificationsQuery query);
}
