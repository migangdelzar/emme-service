package com.emme.studio.documents.adapter.in.web.controller;

import static com.emme.kernel.context.TenantContextHolder.withCurrentTenant;

import com.emme.studio.documents.adapter.in.web.mapper.DocumentWebMapper;
import com.emme.studio.documents.adapter.in.web.request.UploadDocumentRequest;
import com.emme.studio.documents.adapter.in.web.response.DocumentChunkResponse;
import com.emme.studio.documents.adapter.in.web.response.DocumentResponse;
import com.emme.studio.documents.api.command.ProcessDocumentCommand;
import com.emme.studio.documents.api.command.RetireDocumentCommand;
import com.emme.studio.documents.api.command.UploadDocumentCommand;
import com.emme.studio.documents.api.query.GetDocumentChunksQuery;
import com.emme.studio.documents.api.query.GetDocumentQuery;
import com.emme.studio.documents.api.query.ListDocumentsQuery;
import com.emme.studio.documents.api.result.DocumentDetails;
import com.emme.studio.documents.api.usecase.GetDocumentChunksUseCase;
import com.emme.studio.documents.api.usecase.GetDocumentUseCase;
import com.emme.studio.documents.api.usecase.ListDocumentsUseCase;
import com.emme.studio.documents.api.usecase.ProcessDocumentUseCase;
import com.emme.studio.documents.api.usecase.RetireDocumentUseCase;
import com.emme.studio.documents.api.usecase.UploadDocumentUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@RequestMapping(path = "/api/documents", version = "1.0")
@Tag(name = "Documents")
public class DocumentController {

  private final UploadDocumentUseCase uploadDocument;
  private final ListDocumentsUseCase listDocuments;
  private final GetDocumentUseCase getDocument;
  private final ProcessDocumentUseCase processDocument;
  private final RetireDocumentUseCase retireDocument;
  private final GetDocumentChunksUseCase getDocumentChunks;
  private final DocumentWebMapper mapper;

  public DocumentController(
      UploadDocumentUseCase uploadDocument,
      ListDocumentsUseCase listDocuments,
      GetDocumentUseCase getDocument,
      ProcessDocumentUseCase processDocument,
      RetireDocumentUseCase retireDocument,
      GetDocumentChunksUseCase getDocumentChunks,
      DocumentWebMapper mapper) {
    this.uploadDocument = uploadDocument;
    this.listDocuments = listDocuments;
    this.getDocument = getDocument;
    this.processDocument = processDocument;
    this.retireDocument = retireDocument;
    this.getDocumentChunks = getDocumentChunks;
    this.mapper = mapper;
  }

  @PostMapping
  @Operation(summary = "Upload a new document")
  public ResponseEntity<DocumentResponse> upload(
      @Valid @RequestBody UploadDocumentRequest request) {
    return withCurrentTenant(
        tenantId -> {
          DocumentDetails document =
              uploadDocument.upload(
                  new UploadDocumentCommand(tenantId, request.name(), request.sourceType()));
          URI location = URI.create("/api/documents/" + document.id());
          return ResponseEntity.created(location).body(mapper.toResponse(document));
        });
  }

  @GetMapping
  @Operation(summary = "List documents for current tenant")
  public ResponseEntity<List<DocumentResponse>> list() {
    return withCurrentTenant(
        tenantId ->
            ResponseEntity.ok(
                listDocuments.list(new ListDocumentsQuery(tenantId)).stream()
                    .map(mapper::toResponse)
                    .toList()));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a document by ID")
  public ResponseEntity<DocumentResponse> get(@PathVariable UUID id) {
    return withCurrentTenant(
        tenantId -> {
          DocumentDetails document = getDocument.get(new GetDocumentQuery(tenantId, id));
          return ResponseEntity.ok(mapper.toResponse(document));
        });
  }

  @PostMapping("/{id}/process")
  @Operation(summary = "Trigger document processing")
  public ResponseEntity<DocumentResponse> process(@PathVariable UUID id) {
    return withCurrentTenant(
        tenantId -> {
          DocumentDetails document =
              processDocument.process(new ProcessDocumentCommand(tenantId, id));
          return ResponseEntity.ok(mapper.toResponse(document));
        });
  }

  @PostMapping("/{id}/retire")
  @Operation(summary = "Retire a document")
  public ResponseEntity<DocumentResponse> retire(@PathVariable UUID id) {
    return withCurrentTenant(
        tenantId -> {
          DocumentDetails document = retireDocument.retire(new RetireDocumentCommand(tenantId, id));
          return ResponseEntity.ok(mapper.toResponse(document));
        });
  }

  @GetMapping("/{id}/chunks")
  @Operation(summary = "Get document chunks")
  public ResponseEntity<List<DocumentChunkResponse>> getChunks(@PathVariable UUID id) {
    return withCurrentTenant(
        tenantId -> {
          List<DocumentChunkResponse> chunks =
              getDocumentChunks.getChunks(new GetDocumentChunksQuery(tenantId, id)).stream()
                  .map(mapper::toResponse)
                  .toList();
          return ResponseEntity.ok(chunks);
        });
  }
}
