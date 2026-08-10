package com.emme.documents.application.service;

import com.emme.documents.api.command.UploadDocumentCommand;
import com.emme.documents.api.result.DocumentDetails;
import com.emme.documents.api.usecase.UploadDocumentUseCase;
import com.emme.documents.application.mapper.DocumentApplicationMapper;
import com.emme.documents.application.port.out.DocumentRepository;
import com.emme.documents.domain.model.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Executes the UploadDocument use case. */
@Service
@Transactional
public class UploadDocumentService implements UploadDocumentUseCase {

  private final DocumentRepository documentRepository;

  public UploadDocumentService(DocumentRepository documentRepository) {
    this.documentRepository = documentRepository;
  }

  @Override
  public DocumentDetails upload(UploadDocumentCommand command) {
    return DocumentApplicationMapper.toResult(
        documentRepository.save(
            new Document(command.tenantId(), command.name(), command.sourceType())));
  }
}
