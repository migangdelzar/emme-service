package com.emme.assistant.entity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversationEventRepository extends JpaRepository<ConversationEvent, UUID> {
  List<ConversationEvent> findByConversationIdOrderBySequenceNumberAsc(UUID conversationId);

  Optional<ConversationEvent> findTopByConversationIdOrderBySequenceNumberDesc(UUID conversationId);
}
