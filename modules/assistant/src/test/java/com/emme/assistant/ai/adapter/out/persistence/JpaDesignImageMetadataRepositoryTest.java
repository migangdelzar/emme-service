package com.emme.assistant.ai.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.emme.assistant.ai.adapter.out.persistence.entity.DesignImageEntity;
import com.emme.assistant.ai.adapter.out.persistence.repository.SpringDataDesignImageRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class JpaDesignImageMetadataRepositoryTest {

  @Test
  void savesImageMetadataThroughTheModulePrivateSpringDataRepository() {
    SpringDataDesignImageRepository repository = mock(SpringDataDesignImageRepository.class);
    JpaDesignImageMetadataRepository adapter = new JpaDesignImageMetadataRepository(repository);
    UUID tenantId = UUID.randomUUID();
    UUID workflowId = UUID.randomUUID();

    adapter.save(tenantId, workflowId, "design.png", "image/png", 128L);

    ArgumentCaptor<DesignImageEntity> saved = ArgumentCaptor.forClass(DesignImageEntity.class);
    verify(repository).save(saved.capture());
    assertThat(saved.getValue().getTenantId()).isEqualTo(tenantId);
    assertThat(saved.getValue().getWorkflowId()).isEqualTo(workflowId);
    assertThat(saved.getValue().getStorageKey()).isEqualTo("design.png");
    assertThat(saved.getValue().getMediaType()).isEqualTo("image/png");
    assertThat(saved.getValue().getSizeBytes()).isEqualTo(128L);
  }

  @Test
  void deletesImageMetadataUsingTheTenantWorkflowAndStorageKey() {
    SpringDataDesignImageRepository repository = mock(SpringDataDesignImageRepository.class);
    JpaDesignImageMetadataRepository adapter = new JpaDesignImageMetadataRepository(repository);
    UUID tenantId = UUID.randomUUID();
    UUID workflowId = UUID.randomUUID();

    adapter.delete(tenantId, workflowId, "design.png");

    verify(repository)
        .deleteByTenantIdAndWorkflowIdAndStorageKey(tenantId, workflowId, "design.png");
  }
}
