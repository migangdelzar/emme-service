package com.emme.studio.documents.api.usecase;

import com.emme.studio.documents.api.query.ListDocumentsQuery;
import com.emme.studio.documents.api.result.DocumentInfo;
import java.util.List;

public interface ListDocumentsUseCase {
  List<DocumentInfo> list(ListDocumentsQuery query);
}
