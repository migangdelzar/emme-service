package com.emme.studio.documents.api.usecase;

import com.emme.studio.documents.api.query.GetDocumentQuery;
import com.emme.studio.documents.api.result.DocumentDetails;

public interface GetDocumentUseCase {
  DocumentDetails get(GetDocumentQuery query);
}
