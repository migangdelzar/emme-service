package com.emme.documents.api.usecase;

import com.emme.documents.api.query.ListDocumentsQuery;
import com.emme.documents.api.result.DocumentDetails;
import java.util.List;

public interface ListDocumentsUseCase {
  List<DocumentDetails> list(ListDocumentsQuery query);
}
