package com.emme.assistant.entity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
  List<Conversation> findByTenantId(UUID tenantId);

  List<Conversation> findByParticipantId(UUID participantId);

  List<Conversation> findByTenantIdAndStatus(UUID tenantId, ConversationStatus status);

  Optional<Conversation> findByIdAndTenantId(UUID id, UUID tenantId);
}
