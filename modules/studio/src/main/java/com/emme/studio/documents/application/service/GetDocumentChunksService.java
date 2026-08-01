package com.emme.studio.documents.application.service;

import com.emme.studio.documents.api.exception.DocumentNotFoundException;
import com.emme.studio.documents.api.query.GetDocumentChunksQuery;
import com.emme.studio.documents.api.result.DocumentChunkInfo;
import com.emme.studio.documents.api.usecase.GetDocumentChunksUseCase;
import com.emme.studio.documents.application.mapper.DocumentApplicationMapper;
import com.emme.studio.documents.application.port.out.DocumentRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Executes the GetDocumentChunks use case. */
@Service
@Transactional(readOnly = true)
public class GetDocumentChunksService implements GetDocumentChunksUseCase {

  private final DocumentRepository documentRepository;

  public GetDocumentChunksService(DocumentRepository documentRepository) {
    this.documentRepository = documentRepository;
  }

  @Override
  public List<DocumentChunkInfo> getChunks(GetDocumentChunksQuery query) {
    documentRepository
        .findById(query.documentId())
        .orElseThrow(() -> new DocumentNotFoundException(query.documentId()));
    return documentRepository.findChunks(query.documentId()).stream()
        .map(DocumentApplicationMapper::toInfo)
        .toList();
  }
}
