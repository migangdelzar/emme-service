package com.emme.assistant.api.usecase;

import com.emme.assistant.api.query.GetConversationQuery;
import com.emme.assistant.api.result.ConversationInfo;
import java.util.Optional;

public interface GetConversationUseCase {
  Optional<ConversationInfo> get(GetConversationQuery query);
}
