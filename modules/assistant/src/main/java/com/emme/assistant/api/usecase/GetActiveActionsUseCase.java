package com.emme.assistant.api.usecase;

import com.emme.assistant.api.query.GetActiveActionsQuery;
import com.emme.assistant.api.result.PendingActionInfo;
import java.util.List;

public interface GetActiveActionsUseCase {
  List<PendingActionInfo> get(GetActiveActionsQuery query);
}
