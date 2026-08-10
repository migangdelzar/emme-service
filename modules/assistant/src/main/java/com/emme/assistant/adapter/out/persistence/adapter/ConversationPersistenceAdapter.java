package com.emme.assistant.adapter.out.persistence.adapter;

import com.emme.assistant.adapter.out.persistence.entity.ConversationEntity;
import com.emme.assistant.adapter.out.persistence.mapper.ConversationPersistenceMapper;
import com.emme.assistant.adapter.out.persistence.repository.SpringDataConversationRepository;
import com.emme.assistant.application.port.out.ConversationRepository;
import com.emme.assistant.domain.model.Conversation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ConversationPersistenceAdapter implements ConversationRepository {
  private final SpringDataConversationRepository repository;
  private final ConversationPersistenceMapper mapper;

  public ConversationPersistenceAdapter(
      SpringDataConversationRepository repository, ConversationPersistenceMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public Optional<Conversation> findByTenantIdAndId(UUID tenantId, UUID conversationId) {
    return repository.findByIdAndTenantId(conversationId, tenantId).map(mapper::toDomain);
  }

  @Override
  public List<Conversation> findByTenantId(UUID tenantId) {
    return repository.findByTenantId(tenantId).stream().map(mapper::toDomain).toList();
  }

  @Override
  public Conversation save(Conversation conversation) {
    ConversationEntity saved = repository.save(mapper.toEntity(conversation));
    return mapper.toDomain(saved);
  }
}
