package com.emme.assistant.adapter.out.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.assistant.adapter.out.persistence.entity.ConversationEntity;
import com.emme.assistant.adapter.out.persistence.mapper.ConversationPersistenceMapper;
import com.emme.assistant.adapter.out.persistence.repository.SpringDataConversationRepository;
import com.emme.assistant.domain.model.Conversation;
import com.emme.assistant.domain.model.ConversationStatus;
import com.emme.kernel.type.ChannelType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConversationPersistenceAdapterTest {

  @Test
  void listsConversationsFromTheCurrentTenantSchema() {
    SpringDataConversationRepository repository = org.mockito.Mockito.mock();
    ConversationPersistenceAdapter adapter =
        new ConversationPersistenceAdapter(repository, new ConversationPersistenceMapper());
    when(repository.findAll()).thenReturn(List.of());

    assertThat(adapter.findAll()).isEmpty();

    verify(repository).findAll();
  }

  @Test
  void updatesAnExistingConversationThroughTheManagedJpaEntity() {
    SpringDataConversationRepository repository = org.mockito.Mockito.mock();
    ConversationPersistenceAdapter adapter =
        new ConversationPersistenceAdapter(repository, new ConversationPersistenceMapper());
    UUID conversationId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    UUID participantId = UUID.randomUUID();
    ConversationEntity entity =
        new ConversationEntity(tenantId, participantId, ChannelType.WHATSAPP);
    Conversation conversation =
        Conversation.rehydrate(
            conversationId,
            tenantId,
            participantId,
            ChannelType.WHATSAPP,
            ConversationStatus.CLOSED,
            java.time.Instant.now());
    entity.restoreIdentity(conversationId, conversation.startedAt());
    when(repository.findById(conversationId)).thenReturn(Optional.of(entity));
    when(repository.save(entity)).thenReturn(entity);

    Conversation saved = adapter.save(conversation);

    verify(repository).findById(conversationId);
    verify(repository).save(entity);
    assertThat(entity.getStatus()).isEqualTo(ConversationStatus.CLOSED);
    assertThat(saved.status()).isEqualTo(ConversationStatus.CLOSED);
  }
}
