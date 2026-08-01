package com.emme.studio.documents.application.service;

import com.emme.studio.documents.api.command.ProcessDocumentCommand;
import com.emme.studio.documents.api.exception.DocumentNotFoundException;
import com.emme.studio.documents.api.result.DocumentInfo;
import com.emme.studio.documents.api.usecase.ProcessDocumentUseCase;
import com.emme.studio.documents.application.mapper.DocumentApplicationMapper;
import com.emme.studio.documents.application.port.out.DocumentRepository;
import com.emme.studio.documents.domain.model.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Executes the ProcessDocument use case. */
@Service
@Transactional
public class ProcessDocumentService implements ProcessDocumentUseCase {

  private final DocumentRepository documentRepository;

  public ProcessDocumentService(DocumentRepository documentRepository) {
    this.documentRepository = documentRepository;
  }

  @Override
  public DocumentInfo process(ProcessDocumentCommand command) {
    Document document = findDocument(command.documentId());
    document.markProcessing();
    document.markReady();
    return DocumentApplicationMapper.toInfo(documentRepository.save(document));
  }

  private Document findDocument(java.util.UUID documentId) {
    return documentRepository
        .findById(documentId)
        .orElseThrow(() -> new DocumentNotFoundException(documentId));
  }
}
