package com.emme.assistant.adapter.out.persistence.repository;

import com.emme.assistant.adapter.out.persistence.entity.ConversationEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataConversationRepository extends JpaRepository<ConversationEntity, UUID> {
  List<ConversationEntity> findByParticipantId(UUID participantId);
}
