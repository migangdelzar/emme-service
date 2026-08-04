package com.emme.assistant.api.usecase;

import com.emme.assistant.api.query.GetConversationQuery;
import com.emme.assistant.api.result.ConversationDetails;
import java.util.Optional;

public interface GetConversationUseCase {
  Optional<ConversationDetails> get(GetConversationQuery query);
}
