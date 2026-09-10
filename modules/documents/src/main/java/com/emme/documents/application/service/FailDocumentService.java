package com.emme.documents.application.service;

import com.emme.documents.api.command.FailDocumentCommand;
import com.emme.documents.api.exception.DocumentNotFoundException;
import com.emme.documents.api.result.DocumentDetails;
import com.emme.documents.api.usecase.FailDocumentUseCase;
import com.emme.documents.application.mapper.DocumentApplicationMapper;
import com.emme.documents.application.port.out.DocumentRepository;
import com.emme.documents.domain.model.Document;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Executes the FailDocument use case. */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class FailDocumentService implements FailDocumentUseCase {

  private final DocumentRepository documentRepository;

  @Override
  public DocumentDetails fail(FailDocumentCommand command) {
    Document document =
        documentRepository
            .findById(command.documentId())
            .orElseThrow(() -> new DocumentNotFoundException(command.documentId()));
    document.markFailed();
    log.warn("Document {} failed: {}", command.documentId(), command.error());
    return DocumentApplicationMapper.toResult(documentRepository.save(document));
  }
}
