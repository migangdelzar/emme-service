package com.emme.documents.api.usecase;

import com.emme.documents.api.query.GetDocumentQuery;
import com.emme.documents.api.result.DocumentDetails;

public interface GetDocumentUseCase {
  DocumentDetails get(GetDocumentQuery query);
}
