package com.emme.assistant.api.usecase;

import com.emme.assistant.api.query.GetActiveActionsQuery;
import com.emme.assistant.api.result.PendingActionDetails;
import java.util.List;

public interface GetActiveActionsUseCase {
  List<PendingActionDetails> get(GetActiveActionsQuery query);
}
