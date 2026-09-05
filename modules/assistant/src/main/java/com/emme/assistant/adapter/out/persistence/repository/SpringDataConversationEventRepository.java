package com.emme.assistant.adapter.out.persistence.repository;

import com.emme.assistant.adapter.out.persistence.entity.ConversationEventEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataConversationEventRepository
    extends JpaRepository<ConversationEventEntity, UUID> {
  List<ConversationEventEntity> findByConversationIdOrderBySequenceNumberAsc(UUID conversationId);

  Optional<ConversationEventEntity> findTopByConversationIdOrderBySequenceNumberDesc(
      UUID conversationId);
}
