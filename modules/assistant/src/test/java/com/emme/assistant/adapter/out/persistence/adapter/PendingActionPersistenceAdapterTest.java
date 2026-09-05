package com.emme.assistant.adapter.out.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.assistant.adapter.out.persistence.entity.PendingActionEntity;
import com.emme.assistant.adapter.out.persistence.mapper.PendingActionPersistenceMapper;
import com.emme.assistant.adapter.out.persistence.repository.SpringDataPendingActionRepository;
import com.emme.assistant.domain.model.ActionStatus;
import com.emme.assistant.domain.model.ActionType;
import com.emme.assistant.domain.model.PendingAction;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PendingActionPersistenceAdapterTest {

  private final SpringDataPendingActionRepository repository =
      mock(SpringDataPendingActionRepository.class);
  private final PendingActionPersistenceAdapter adapter =
      new PendingActionPersistenceAdapter(repository, new PendingActionPersistenceMapper());

  @Test
  void readsPendingActionsUsingTheSchemaLocalConversationKeyAndStatus() {
    UUID conversationId = UUID.randomUUID();
    PendingActionEntity entity =
        new PendingActionEntity(
            UUID.randomUUID(),
            conversationId,
            ActionType.BOOK,
            "booking details",
            Instant.parse("2026-09-06T00:00:00Z"));
    when(repository.findByConversationIdAndStatusOrderByCreatedAtAscIdAsc(
            conversationId, ActionStatus.PENDING))
        .thenReturn(List.of(entity));

    var found = adapter.findByConversationIdAndStatus(conversationId, ActionStatus.PENDING);

    assertThat(found)
        .singleElement()
        .satisfies(
            action -> {
              assertThat(action.conversationId()).isEqualTo(conversationId);
              assertThat(action.status()).isEqualTo(ActionStatus.PENDING);
            });
  }

  @Test
  void updatesAnExistingPendingActionThroughTheManagedJpaEntity() {
    UUID actionId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();
    PendingActionEntity entity =
        new PendingActionEntity(
            tenantId,
            conversationId,
            ActionType.BOOK,
            "before",
            Instant.parse("2026-09-06T00:00:00Z"));
    PendingAction action =
        new PendingAction(
            actionId,
            tenantId,
            conversationId,
            ActionType.BOOK,
            ActionStatus.CONFIRMED,
            "after",
            Instant.parse("2026-09-07T00:00:00Z"),
            Instant.parse("2026-09-05T00:00:00Z"));
    when(repository.findById(actionId)).thenReturn(java.util.Optional.of(entity));
    when(repository.save(entity)).thenReturn(entity);

    PendingAction saved = adapter.save(action);

    verify(repository).findById(actionId);
    verify(repository).save(entity);
    assertThat(entity.getStatus()).isEqualTo(ActionStatus.CONFIRMED);
    assertThat(entity.getDetails()).isEqualTo("after");
    assertThat(entity.getExpiresAt()).isEqualTo(Instant.parse("2026-09-07T00:00:00Z"));
    assertThat(saved.status()).isEqualTo(ActionStatus.CONFIRMED);
  }
}
