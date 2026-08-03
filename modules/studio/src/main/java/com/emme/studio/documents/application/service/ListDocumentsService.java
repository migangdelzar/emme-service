package com.emme.studio.documents.application.service;

import com.emme.studio.documents.api.query.ListDocumentsQuery;
import com.emme.studio.documents.api.result.DocumentInfo;
import com.emme.studio.documents.api.usecase.ListDocumentsUseCase;
import com.emme.studio.documents.application.mapper.DocumentApplicationMapper;
import com.emme.studio.documents.application.port.out.DocumentRepository;
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
  public List<DocumentInfo> list(ListDocumentsQuery query) {
    return documentRepository.findByTenantId(query.tenantId()).stream()
        .map(DocumentApplicationMapper::toInfo)
        .toList();
  }
}
