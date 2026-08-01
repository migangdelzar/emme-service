package com.emme.studio.documents.api.usecase;

import com.emme.studio.documents.api.query.GetDocumentQuery;
import com.emme.studio.documents.api.result.DocumentInfo;

public interface GetDocumentUseCase {
  DocumentInfo get(GetDocumentQuery query);
}
