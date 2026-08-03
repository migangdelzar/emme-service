package com.emme.catalog.application.service;

import com.emme.assistant.ai.api.usecase.CaptionImageUseCase;
import com.emme.assistant.ai.api.usecase.EmbedTextUseCase;
import com.emme.catalog.api.query.MatchCatalogItemsQuery;
import com.emme.catalog.api.result.CatalogMatchInfo;
import com.emme.catalog.api.result.CatalogMatchListInfo;
import com.emme.catalog.api.result.MatchedImageInfo;
import com.emme.catalog.api.usecase.MatchCatalogItemsUseCase;
import com.emme.catalog.application.port.out.CatalogItemImageRepository;
import com.emme.catalog.application.port.out.CatalogItemRepository;
import com.emme.catalog.application.port.out.CatalogSearchHit;
import com.emme.catalog.application.port.out.CatalogSearchPort;
import com.emme.catalog.domain.model.CatalogItem;
import com.emme.catalog.domain.model.CatalogItemImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Answers "how much does this cost?": query (+ optional photo) → hybrid search over catalog items
 * and their reference-image captions → top priced matches. Image hits contribute their parent
 * item's score.
 */
@Service
@Transactional(readOnly = true)
public class MatchCatalogItemsService implements MatchCatalogItemsUseCase {

  private static final int BRANCH_K = 10;

  private final CaptionImageUseCase captionImageUseCase;
  private final EmbedTextUseCase embedTextUseCase;
  private final CatalogSearchPort searchPort;
  private final CatalogItemRepository itemRepository;
  private final CatalogItemImageRepository imageRepository;

  public MatchCatalogItemsService(
      CaptionImageUseCase captionImageUseCase,
      EmbedTextUseCase embedTextUseCase,
      CatalogSearchPort searchPort,
      CatalogItemRepository itemRepository,
      CatalogItemImageRepository imageRepository) {
    this.captionImageUseCase = captionImageUseCase;
    this.embedTextUseCase = embedTextUseCase;
    this.searchPort = searchPort;
    this.itemRepository = itemRepository;
    this.imageRepository = imageRepository;
  }

  @Override
  public CatalogMatchListInfo match(MatchCatalogItemsQuery query) {
    UUID tenantId = query.tenantId();

    // 1. Build combined query text (caption the image if provided)
    String queryText = query.query();
    String imageBase64 = query.imageBase64();
    if (imageBase64 != null && !imageBase64.isBlank()) {
      String caption = captionImageUseCase.caption(imageBase64);
      if (!caption.isBlank()) queryText = queryText + " " + caption;
    }

    // 2. Embed the combined query
    List<Float> queryVec = embedTextUseCase.embed(queryText);

    // 3. Hybrid search over catalog items
    List<CatalogSearchHit> itemHits =
        searchPort.searchCatalogItems(tenantId, queryVec, queryText, BRANCH_K);

    // 4. Hybrid search over catalog item images
    List<CatalogSearchHit> imageHits =
        searchPort.searchCatalogItemImages(tenantId, queryVec, queryText, BRANCH_K);

    // 5. Aggregate scores: item hits + image hits (mapped to their parent item)
    Map<UUID, Double> itemScores = new HashMap<>();
    Map<UUID, List<MatchedImageInfo>> itemImages = new HashMap<>();

    for (CatalogSearchHit hit : itemHits) {
      itemScores.merge(hit.id(), hit.score(), Double::sum);
    }

    if (!imageHits.isEmpty()) {
      Map<UUID, Double> imageScoreMap = new HashMap<>();
      for (CatalogSearchHit hit : imageHits) imageScoreMap.put(hit.id(), hit.score());

      for (CatalogItemImage img :
          imageRepository.findAllById(imageHits.stream().map(CatalogSearchHit::id).toList())) {
        UUID parentId = img.getCatalogItemId();
        double imgScore = imageScoreMap.getOrDefault(img.getId(), 0.0);
        itemScores.merge(parentId, imgScore, Double::sum);
        itemImages
            .computeIfAbsent(parentId, k -> new ArrayList<>())
            .add(new MatchedImageInfo(img.getId(), img.getStorageKey()));
      }
    }

    if (itemScores.isEmpty()) return new CatalogMatchListInfo(List.of());

    // 6. Load catalog items with tenant isolation check
    Map<UUID, CatalogItem> itemsById = new HashMap<>();
    for (CatalogItem item : itemRepository.findByTenantId(tenantId)) {
      if (itemScores.containsKey(item.getId())) {
        itemsById.put(item.getId(), item);
      }
    }

    // 7. Build results sorted by score descending
    List<CatalogMatchInfo> results = new ArrayList<>();
    for (var entry : itemScores.entrySet()) {
      CatalogItem item = itemsById.get(entry.getKey());
      if (item == null) continue;
      results.add(
          new CatalogMatchInfo(
              item.getId(),
              item.getName(),
              item.getPrice(),
              entry.getValue(),
              itemImages.getOrDefault(entry.getKey(), List.of())));
    }

    results.sort(Comparator.comparingDouble(CatalogMatchInfo::score).reversed());
    return new CatalogMatchListInfo(results);
  }
}
