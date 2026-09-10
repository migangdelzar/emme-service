package com.emme.documents.application.service;

import com.emme.documents.api.exception.DocumentNotFoundException;
import com.emme.documents.api.query.GetDocumentChunksQuery;
import com.emme.documents.api.result.DocumentChunkDetails;
import com.emme.documents.api.usecase.GetDocumentChunksUseCase;
import com.emme.documents.application.mapper.DocumentApplicationMapper;
import com.emme.documents.application.port.out.DocumentRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Executes the GetDocumentChunks use case. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetDocumentChunksService implements GetDocumentChunksUseCase {

  private final DocumentRepository documentRepository;

  @Override
  public List<DocumentChunkDetails> getChunks(GetDocumentChunksQuery query) {
    documentRepository
        .findById(query.documentId())
        .orElseThrow(() -> new DocumentNotFoundException(query.documentId()));
    return documentRepository.findChunks(query.documentId()).stream()
        .map(DocumentApplicationMapper::toResult)
        .toList();
  }
}
