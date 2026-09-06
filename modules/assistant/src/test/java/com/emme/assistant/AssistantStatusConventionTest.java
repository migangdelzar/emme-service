package com.emme.assistant;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.assistant.adapter.in.web.response.ConversationResponse;
import com.emme.assistant.adapter.in.web.response.PendingActionResponse;
import com.emme.assistant.api.type.ActionStatus;
import com.emme.assistant.api.type.ConversationStatus;
import org.junit.jupiter.api.Test;

class AssistantStatusConventionTest {
  @Test
  void conversationResponseUsesThePublicStatusEnum() {
    assertThat(ConversationResponse.class.getRecordComponents()[4].getType())
        .isEqualTo(ConversationStatus.class);
  }

  @Test
  void pendingActionResponseUsesThePublicStatusEnum() {
    assertThat(PendingActionResponse.class.getRecordComponents()[3].getType())
        .isEqualTo(ActionStatus.class);
  }
}
