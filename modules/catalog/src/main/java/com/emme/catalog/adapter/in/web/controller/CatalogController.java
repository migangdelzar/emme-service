package com.emme.catalog.adapter.in.web.controller;

import static com.emme.kernel.context.TenantContextHolder.withCurrentTenant;

import com.emme.catalog.adapter.in.web.mapper.CatalogWebMapper;
import com.emme.catalog.adapter.in.web.request.AddCatalogItemImageRequest;
import com.emme.catalog.adapter.in.web.request.CreateCatalogItemRequest;
import com.emme.catalog.adapter.in.web.request.MatchCatalogItemsRequest;
import com.emme.catalog.adapter.in.web.response.CatalogItemImageResponse;
import com.emme.catalog.adapter.in.web.response.CatalogItemResponse;
import com.emme.catalog.adapter.in.web.response.CatalogMatchResponse;
import com.emme.catalog.api.result.CatalogItemImageInfo;
import com.emme.catalog.api.result.CatalogItemInfo;
import com.emme.catalog.api.usecase.AddCatalogItemImageUseCase;
import com.emme.catalog.api.usecase.CreateCatalogItemUseCase;
import com.emme.catalog.api.usecase.DeleteCatalogItemUseCase;
import com.emme.catalog.api.usecase.ListCatalogItemsUseCase;
import com.emme.catalog.api.usecase.MatchCatalogItemsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/catalog", version = "1.0")
@Tag(name = "Catalog")
public class CatalogController {

  private final CreateCatalogItemUseCase createCatalogItemUseCase;
  private final DeleteCatalogItemUseCase deleteCatalogItemUseCase;
  private final ListCatalogItemsUseCase listCatalogItemsUseCase;
  private final AddCatalogItemImageUseCase addCatalogItemImageUseCase;
  private final MatchCatalogItemsUseCase matchCatalogItemsUseCase;

  public CatalogController(
      CreateCatalogItemUseCase createCatalogItemUseCase,
      DeleteCatalogItemUseCase deleteCatalogItemUseCase,
      ListCatalogItemsUseCase listCatalogItemsUseCase,
      AddCatalogItemImageUseCase addCatalogItemImageUseCase,
      MatchCatalogItemsUseCase matchCatalogItemsUseCase) {
    this.createCatalogItemUseCase = createCatalogItemUseCase;
    this.deleteCatalogItemUseCase = deleteCatalogItemUseCase;
    this.listCatalogItemsUseCase = listCatalogItemsUseCase;
    this.addCatalogItemImageUseCase = addCatalogItemImageUseCase;
    this.matchCatalogItemsUseCase = matchCatalogItemsUseCase;
  }

  @PostMapping("/items")
  @Operation(summary = "Create a priced catalog item under a service")
  public ResponseEntity<CatalogItemResponse> create(
      @Valid @RequestBody CreateCatalogItemRequest request) {
    return withCurrentTenant(
        tenantId -> {
          CatalogItemInfo info =
              createCatalogItemUseCase.create(CatalogWebMapper.toCommand(tenantId, request));
          return ResponseEntity.created(URI.create("/api/catalog/items/" + info.id()))
              .body(CatalogWebMapper.toResponse(info));
        });
  }

  @GetMapping("/items")
  @Operation(summary = "List catalog items for the tenant")
  public List<CatalogItemResponse> list() {
    return withCurrentTenant(
        tenantId ->
            listCatalogItemsUseCase.list(CatalogWebMapper.toQuery(tenantId)).stream()
                .map(CatalogWebMapper::toResponse)
                .toList());
  }

  @DeleteMapping("/items/{id}")
  @Operation(summary = "Delete a catalog item and its images")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    return withCurrentTenant(
        tenantId -> {
          deleteCatalogItemUseCase.delete(CatalogWebMapper.toDeleteCommand(tenantId, id));
          return ResponseEntity.noContent().build();
        });
  }

  @PostMapping("/items/{id}/images")
  @Operation(summary = "Attach a reference image; captioned by the vision model")
  public ResponseEntity<CatalogItemImageResponse> addImage(
      @PathVariable UUID id, @Valid @RequestBody AddCatalogItemImageRequest request) {
    return withCurrentTenant(
        tenantId -> {
          CatalogItemImageInfo info =
              addCatalogItemImageUseCase.addImage(
                  CatalogWebMapper.toCommand(tenantId, id, request));
          return ResponseEntity.created(
                  URI.create("/api/catalog/items/" + id + "/images/" + info.id()))
              .body(CatalogWebMapper.toResponse(info));
        });
  }

  @PostMapping("/match")
  @Operation(summary = "Match a customer message (and optional photo) to priced catalog items")
  public CatalogMatchResponse match(@Valid @RequestBody MatchCatalogItemsRequest request) {
    return withCurrentTenant(
        tenantId ->
            CatalogWebMapper.toResponse(
                matchCatalogItemsUseCase.match(CatalogWebMapper.toQuery(tenantId, request))));
  }
}
