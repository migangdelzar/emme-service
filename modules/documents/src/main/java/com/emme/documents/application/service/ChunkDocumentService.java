package com.emme.documents.application.service;

import com.emme.documents.api.command.ChunkDocumentCommand;
import com.emme.documents.api.exception.DocumentNotFoundException;
import com.emme.documents.api.result.DocumentChunkDetails;
import com.emme.documents.api.usecase.ChunkDocumentUseCase;
import com.emme.documents.application.mapper.DocumentApplicationMapper;
import com.emme.documents.application.port.out.DocumentRepository;
import com.emme.documents.domain.model.Document;
import com.emme.documents.domain.model.DocumentChunk;
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
  public List<DocumentChunkDetails> chunk(ChunkDocumentCommand command) {
    Document document =
        documentRepository
            .findById(command.documentId())
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
    return chunks.stream().map(DocumentApplicationMapper::toResult).toList();
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
