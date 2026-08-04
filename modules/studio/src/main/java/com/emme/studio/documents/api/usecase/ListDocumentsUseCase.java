package com.emme.studio.documents.api.usecase;

import com.emme.studio.documents.api.query.ListDocumentsQuery;
import com.emme.studio.documents.api.result.DocumentDetails;
import java.util.List;

public interface ListDocumentsUseCase {
  List<DocumentDetails> list(ListDocumentsQuery query);
}
