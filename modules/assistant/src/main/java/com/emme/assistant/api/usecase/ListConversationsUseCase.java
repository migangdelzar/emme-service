package com.emme.assistant.api.usecase;

import com.emme.assistant.api.query.ListConversationsQuery;
import com.emme.assistant.api.result.ConversationDetails;
import java.util.List;

public interface ListConversationsUseCase {
  List<ConversationDetails> list(ListConversationsQuery query);
}
