package com.emme.assistant.adapter.out.persistence.repository;

import com.emme.assistant.adapter.out.persistence.entity.ConversationEntity;
import com.emme.assistant.domain.model.ConversationStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataConversationRepository extends JpaRepository<ConversationEntity, UUID> {
  List<ConversationEntity> findByTenantId(UUID tenantId);

  List<ConversationEntity> findByParticipantId(UUID participantId);

  List<ConversationEntity> findByTenantIdAndStatus(UUID tenantId, ConversationStatus status);
}
