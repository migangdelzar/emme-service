package com.emme.documents.application.service;

import com.emme.documents.api.query.ListDocumentsQuery;
import com.emme.documents.api.result.DocumentDetails;
import com.emme.documents.api.usecase.ListDocumentsUseCase;
import com.emme.documents.application.mapper.DocumentApplicationMapper;
import com.emme.documents.application.port.out.DocumentRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Executes the ListDocuments use case. */
@Service
@Transactional(readOnly = true)
public class ListDocumentsService implements ListDocumentsUseCase {

  private final DocumentRepository documentRepository;

  public ListDocumentsService(DocumentRepository documentRepository) {
    this.documentRepository = documentRepository;
  }

  @Override
  public List<DocumentDetails> list(ListDocumentsQuery query) {
    return documentRepository.findAll().stream().map(DocumentApplicationMapper::toResult).toList();
  }
}
