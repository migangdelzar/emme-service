package com.emme.assistant.api.usecase;

import com.emme.assistant.api.query.ListConversationsQuery;
import com.emme.assistant.api.result.ConversationInfo;
import java.util.List;

public interface ListConversationsUseCase {
  List<ConversationInfo> list(ListConversationsQuery query);
}
