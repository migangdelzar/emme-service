package com.emme.studio.documents.api.usecase;

import com.emme.studio.documents.api.command.RetireDocumentCommand;
import com.emme.studio.documents.api.result.DocumentDetails;

public interface RetireDocumentUseCase {
  DocumentDetails retire(RetireDocumentCommand command);
}
