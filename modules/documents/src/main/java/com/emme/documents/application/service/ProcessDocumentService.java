package com.emme.documents.application.service;

import com.emme.documents.api.command.ProcessDocumentCommand;
import com.emme.documents.api.exception.DocumentNotFoundException;
import com.emme.documents.api.result.DocumentDetails;
import com.emme.documents.api.usecase.ProcessDocumentUseCase;
import com.emme.documents.application.mapper.DocumentApplicationMapper;
import com.emme.documents.application.port.out.DocumentRepository;
import com.emme.documents.domain.model.Document;
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
  public DocumentDetails process(ProcessDocumentCommand command) {
    Document document = findDocument(command.documentId());
    document.markProcessing();
    document.markReady();
    return DocumentApplicationMapper.toResult(documentRepository.save(document));
  }

  private Document findDocument(java.util.UUID documentId) {
    return documentRepository
        .findById(documentId)
        .orElseThrow(() -> new DocumentNotFoundException(documentId));
  }
}
