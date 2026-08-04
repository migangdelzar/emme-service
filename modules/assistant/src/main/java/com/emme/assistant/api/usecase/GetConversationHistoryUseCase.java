package com.emme.assistant.api.usecase;

import com.emme.assistant.api.query.GetConversationHistoryQuery;
import com.emme.assistant.api.result.ConversationEventDetails;
import java.util.List;

public interface GetConversationHistoryUseCase {
  List<ConversationEventDetails> get(GetConversationHistoryQuery query);
}
