package com.emme.studio.documents.application.service;

import com.emme.studio.documents.api.exception.DocumentNotFoundException;
import com.emme.studio.documents.api.query.GetDocumentQuery;
import com.emme.studio.documents.api.result.DocumentDetails;
import com.emme.studio.documents.api.usecase.GetDocumentUseCase;
import com.emme.studio.documents.application.mapper.DocumentApplicationMapper;
import com.emme.studio.documents.application.port.out.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Executes the GetDocument use case. */
@Service
@Transactional(readOnly = true)
public class GetDocumentService implements GetDocumentUseCase {

  private final DocumentRepository documentRepository;

  public GetDocumentService(DocumentRepository documentRepository) {
    this.documentRepository = documentRepository;
  }

  @Override
  public DocumentDetails get(GetDocumentQuery query) {
    return documentRepository
        .findByTenantIdAndId(query.tenantId(), query.documentId())
        .map(DocumentApplicationMapper::toResult)
        .orElseThrow(() -> new DocumentNotFoundException(query.documentId()));
  }
}
