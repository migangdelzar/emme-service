package com.emme.assistant.adapter.out.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.assistant.adapter.out.persistence.mapper.ConversationPersistenceMapper;
import com.emme.assistant.adapter.out.persistence.repository.SpringDataConversationRepository;
import java.util.List;
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
}
