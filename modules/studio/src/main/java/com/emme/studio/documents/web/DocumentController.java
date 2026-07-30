package com.emme.studio.documents.web;

import static com.emme.kernel.context.TenantContextHolder.withCurrentTenant;

import com.emme.studio.documents.application.DocumentService;
import com.emme.studio.documents.entity.Document;
import com.emme.studio.documents.entity.DocumentChunk;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/documents")
@Tag(name = "Documents")
public class DocumentController {

  private final DocumentService documentService;

  public DocumentController(DocumentService documentService) {
    this.documentService = documentService;
  }

  @PostMapping
  @Operation(summary = "Upload a new document")
  public ResponseEntity<DocumentResponse> upload(
      @Valid @RequestBody UploadDocumentRequest request) {
    return withCurrentTenant(
        tenantId -> {
          Document document =
              documentService.upload(tenantId, request.name(), request.sourceType());
          URI location = URI.create("/api/v1/documents/" + document.getId());
          return ResponseEntity.created(location).body(DocumentResponse.from(document));
        });
  }

  @GetMapping
  @Operation(summary = "List documents for current tenant")
  public ResponseEntity<List<DocumentResponse>> list() {
    return withCurrentTenant(
        tenantId ->
            ResponseEntity.ok(
                documentService.findByTenantId(tenantId).stream()
                    .map(DocumentResponse::from)
                    .toList()));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a document by ID")
  public ResponseEntity<DocumentResponse> get(@PathVariable UUID id) {
    Document document = documentService.findById(id);
    return ResponseEntity.ok(DocumentResponse.from(document));
  }

  @PostMapping("/{id}/process")
  @Operation(summary = "Trigger document processing")
  public ResponseEntity<DocumentResponse> process(@PathVariable UUID id) {
    Document document = documentService.process(id);
    return ResponseEntity.ok(DocumentResponse.from(document));
  }

  @PostMapping("/{id}/retire")
  @Operation(summary = "Retire a document")
  public ResponseEntity<DocumentResponse> retire(@PathVariable UUID id) {
    Document document = documentService.retire(id);
    return ResponseEntity.ok(DocumentResponse.from(document));
  }

  @GetMapping("/{id}/chunks")
  @Operation(summary = "Get document chunks")
  public ResponseEntity<List<DocumentChunkResponse>> getChunks(@PathVariable UUID id) {
    List<DocumentChunkResponse> chunks =
        documentService.getChunks(id).stream().map(DocumentChunkResponse::from).toList();
    return ResponseEntity.ok(chunks);
  }

  // --- DTOs ---

  public record UploadDocumentRequest(@NotBlank String name, @NotBlank String sourceType) {}

  public record DocumentResponse(
      UUID id, UUID tenantId, String name, String sourceType, String status, int version) {
    public static DocumentResponse from(Document d) {
      return new DocumentResponse(
          d.getId(),
          d.getTenantId(),
          d.getName(),
          d.getSourceType(),
          d.getStatus().name(),
          d.getVersion());
    }
  }

  public record DocumentChunkResponse(
      UUID id, UUID documentId, int chunkIndex, String content, String contentFingerprint) {
    public static DocumentChunkResponse from(DocumentChunk c) {
      return new DocumentChunkResponse(
          c.getId(),
          c.getDocumentId(),
          c.getChunkIndex(),
          c.getContent(),
          c.getContentFingerprint());
    }
  }
}
