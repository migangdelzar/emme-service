package com.emme.assistant.adapter.out.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emme.assistant.adapter.out.persistence.entity.ConversationEventEntity;
import com.emme.assistant.adapter.out.persistence.mapper.ConversationEventPersistenceMapper;
import com.emme.assistant.adapter.out.persistence.repository.SpringDataConversationEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConversationEventPersistenceAdapterTest {

  private final SpringDataConversationEventRepository repository =
      mock(SpringDataConversationEventRepository.class);
  private final ConversationEventPersistenceAdapter adapter =
      new ConversationEventPersistenceAdapter(
          repository, new ConversationEventPersistenceMapper(new ObjectMapper()));

  @Test
  void readsTheLatestEventUsingTheSchemaLocalConversationKey() {
    UUID conversationId = UUID.randomUUID();
    ConversationEventEntity entity =
        new ConversationEventEntity(UUID.randomUUID(), conversationId, 2, "assistant", "reply");
    when(repository.findTopByConversationIdOrderBySequenceNumberDesc(conversationId))
        .thenReturn(Optional.of(entity));

    var found = adapter.findLatestByConversationId(conversationId);

    assertThat(found).isPresent();
    assertThat(found.orElseThrow().conversationId()).isEqualTo(conversationId);
    assertThat(found.orElseThrow().sequenceNumber()).isEqualTo(2);
  }

  @Test
  void readsConversationHistoryUsingTheSchemaLocalConversationKey() {
    UUID conversationId = UUID.randomUUID();
    ConversationEventEntity entity =
        new ConversationEventEntity(UUID.randomUUID(), conversationId, 1, "user", "question");
    when(repository.findByConversationIdOrderBySequenceNumberAsc(conversationId))
        .thenReturn(List.of(entity));

    var found = adapter.findByConversationId(conversationId);

    assertThat(found)
        .singleElement()
        .satisfies(event -> assertThat(event.sequenceNumber()).isEqualTo(1));
  }
}
