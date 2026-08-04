package com.emme.studio.documents.application.service;

import com.emme.studio.documents.api.command.RetireDocumentCommand;
import com.emme.studio.documents.api.exception.DocumentNotFoundException;
import com.emme.studio.documents.api.result.DocumentDetails;
import com.emme.studio.documents.api.usecase.RetireDocumentUseCase;
import com.emme.studio.documents.application.mapper.DocumentApplicationMapper;
import com.emme.studio.documents.application.port.out.DocumentRepository;
import com.emme.studio.documents.domain.model.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Executes the RetireDocument use case. */
@Service
@Transactional
public class RetireDocumentService implements RetireDocumentUseCase {

  private final DocumentRepository documentRepository;

  public RetireDocumentService(DocumentRepository documentRepository) {
    this.documentRepository = documentRepository;
  }

  @Override
  public DocumentDetails retire(RetireDocumentCommand command) {
    Document document =
        documentRepository
            .findByTenantIdAndId(command.tenantId(), command.documentId())
            .orElseThrow(() -> new DocumentNotFoundException(command.documentId()));
    document.markRetired();
    return DocumentApplicationMapper.toResult(documentRepository.save(document));
  }
}
