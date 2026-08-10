package com.emme.documents.application.service;

import com.emme.documents.api.command.FailDocumentCommand;
import com.emme.documents.api.exception.DocumentNotFoundException;
import com.emme.documents.api.result.DocumentDetails;
import com.emme.documents.api.usecase.FailDocumentUseCase;
import com.emme.documents.application.mapper.DocumentApplicationMapper;
import com.emme.documents.application.port.out.DocumentRepository;
import com.emme.documents.domain.model.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Executes the FailDocument use case. */
@Service
@Transactional
public class FailDocumentService implements FailDocumentUseCase {

  private static final Logger log = LoggerFactory.getLogger(FailDocumentService.class);
  private final DocumentRepository documentRepository;

  public FailDocumentService(DocumentRepository documentRepository) {
    this.documentRepository = documentRepository;
  }

  @Override
  public DocumentDetails fail(FailDocumentCommand command) {
    Document document =
        documentRepository
            .findByTenantIdAndId(command.tenantId(), command.documentId())
            .orElseThrow(() -> new DocumentNotFoundException(command.documentId()));
    document.markFailed();
    log.warn("Document {} failed: {}", command.documentId(), command.error());
    return DocumentApplicationMapper.toResult(documentRepository.save(document));
  }
}
