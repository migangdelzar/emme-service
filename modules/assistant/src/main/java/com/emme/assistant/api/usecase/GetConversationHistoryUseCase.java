package com.emme.assistant.api.usecase;

import com.emme.assistant.api.query.GetConversationHistoryQuery;
import com.emme.assistant.api.result.ConversationEventInfo;
import java.util.List;

public interface GetConversationHistoryUseCase {
  List<ConversationEventInfo> get(GetConversationHistoryQuery query);
}
