package com.emme.studio.documents.application.service;

import com.emme.studio.documents.api.command.ChunkDocumentCommand;
import com.emme.studio.documents.api.exception.DocumentNotFoundException;
import com.emme.studio.documents.api.result.DocumentChunkInfo;
import com.emme.studio.documents.api.usecase.ChunkDocumentUseCase;
import com.emme.studio.documents.application.mapper.DocumentApplicationMapper;
import com.emme.studio.documents.application.port.out.DocumentRepository;
import com.emme.studio.documents.domain.model.Document;
import com.emme.studio.documents.domain.model.DocumentChunk;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Executes the ChunkDocument use case. */
@Service
@Transactional
public class ChunkDocumentService implements ChunkDocumentUseCase {

  private final DocumentRepository documentRepository;

  public ChunkDocumentService(DocumentRepository documentRepository) {
    this.documentRepository = documentRepository;
  }

  @Override
  public List<DocumentChunkInfo> chunk(ChunkDocumentCommand command) {
    Document document =
        documentRepository
            .findByTenantIdAndId(command.tenantId(), command.documentId())
            .orElseThrow(() -> new DocumentNotFoundException(command.documentId()));
    List<DocumentChunk> chunks =
        java.util.stream.IntStream.range(0, command.chunks().size())
            .mapToObj(
                index -> {
                  String content = command.chunks().get(index);
                  return new DocumentChunk(
                      document.tenantId(), document.id(), index, content, sha256(content));
                })
            .toList();
    documentRepository.replaceChunks(command.tenantId(), command.documentId(), chunks);
    return chunks.stream().map(DocumentApplicationMapper::toInfo).toList();
  }

  private static String sha256(String input) {
    try {
      byte[] hash =
          MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 not available", exception);
    }
  }
}
