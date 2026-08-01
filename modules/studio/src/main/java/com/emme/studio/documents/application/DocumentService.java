package com.emme.studio.documents.application;

import com.emme.studio.documents.application.port.out.DocumentRepository;
import com.emme.studio.documents.domain.model.Document;
import com.emme.studio.documents.domain.model.DocumentChunk;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DocumentService {

  private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

  private final DocumentRepository documentRepository;

  public DocumentService(DocumentRepository documentRepository) {
    this.documentRepository = documentRepository;
  }

  /** Upload a new document. Status: UPLOADED */
  public Document upload(UUID tenantId, String name, String sourceType) {
    Document document = new Document(tenantId, name, sourceType);
    return documentRepository.save(document);
  }

  /** Process a document: UPLOADED → PROCESSING → READY */
  public Document process(UUID documentId) {
    Document document = findDocumentOrThrow(documentId);
    document.markProcessing();
    // In real app, actual processing would happen asynchronously.
    // Here we simulate immediate success.
    document.markReady();
    return documentRepository.save(document);
  }

  /** Mark a document as FAILED */
  public Document fail(UUID documentId, String error) {
    Document document = findDocumentOrThrow(documentId);
    document.markFailed();
    log.warn("Document {} failed: {}", documentId, error);
    return documentRepository.save(document);
  }

  /** Retire a document */
  public Document retire(UUID documentId) {
    Document document = findDocumentOrThrow(documentId);
    document.markRetired();
    return documentRepository.save(document);
  }

  /** Chunk a document into multiple chunks */
  public List<DocumentChunk> chunkDocument(UUID documentId, List<String> chunks) {
    Document document = findDocumentOrThrow(documentId);
    List<DocumentChunk> documentChunks = new java.util.ArrayList<>();
    for (int i = 0; i < chunks.size(); i++) {
      String content = chunks.get(i);
      String fingerprint = sha256(content);
      DocumentChunk chunk =
          new DocumentChunk(document.tenantId(), document.id(), i, content, fingerprint);
      documentChunks.add(chunk);
    }
    documentRepository.replaceChunks(documentId, documentChunks);
    return documentChunks;
  }

  @Transactional(readOnly = true)
  public List<Document> findByTenantId(UUID tenantId) {
    return documentRepository.findByTenantId(tenantId);
  }

  @Transactional(readOnly = true)
  public List<DocumentChunk> getChunks(UUID documentId) {
    return documentRepository.findChunks(documentId);
  }

  @Transactional(readOnly = true)
  public Document findById(UUID documentId) {
    return findDocumentOrThrow(documentId);
  }

  private Document findDocumentOrThrow(UUID documentId) {
    return documentRepository
        .findById(documentId)
        .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
  }

  private static String sha256(String input) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }
}
