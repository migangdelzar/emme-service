package com.emme.catalog.adapter.out.client.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emme.catalog.application.port.out.CatalogSearchHit;
import com.emme.shared.search.HybridSearch;
import com.emme.shared.search.SearchTarget;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class HybridCatalogSearchAdapterTest {

  private final HybridSearch hybridSearch = mock(HybridSearch.class);
  private final HybridCatalogSearchAdapter adapter = new HybridCatalogSearchAdapter(hybridSearch);

  @Test
  void translatesCatalogItemHitsWithoutExposingSharedSearchTypes() {
    UUID tenantId = UUID.randomUUID();
    UUID itemId = UUID.randomUUID();
    when(hybridSearch.search(
            eq(SearchTarget.CATALOG_ITEM), eq(tenantId), anyList(), eq("gel"), eq(10)))
        .thenReturn(List.of(new HybridSearch.Scored(itemId, 0.91d)));

    List<CatalogSearchHit> hits = adapter.searchCatalogItems(tenantId, List.of(1.0f), "gel", 10);

    assertThat(hits).containsExactly(new CatalogSearchHit(itemId, 0.91d));
  }

  @Test
  void translatesCatalogImageHitsWithTheImageSearchTarget() {
    UUID tenantId = UUID.randomUUID();
    UUID imageId = UUID.randomUUID();
    when(hybridSearch.search(
            eq(SearchTarget.CATALOG_ITEM_IMAGE), eq(tenantId), anyList(), eq("gel"), eq(10)))
        .thenReturn(List.of(new HybridSearch.Scored(imageId, 0.73d)));

    List<CatalogSearchHit> hits =
        adapter.searchCatalogItemImages(tenantId, List.of(1.0f), "gel", 10);

    assertThat(hits).containsExactly(new CatalogSearchHit(imageId, 0.73d));
  }
}
