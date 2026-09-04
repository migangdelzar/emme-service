package com.emme.assistant.ai.adapter.out.persistence.repository;

import com.emme.assistant.ai.adapter.out.persistence.entity.ConversationWorkflowReviewDecisionEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Module-private Spring Data repository for conversation workflow review audits. */
@Repository
public interface SpringDataConversationWorkflowReviewDecisionRepository
    extends JpaRepository<ConversationWorkflowReviewDecisionEntity, UUID> {}
