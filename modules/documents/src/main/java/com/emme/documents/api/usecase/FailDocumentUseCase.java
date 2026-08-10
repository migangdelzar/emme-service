package com.emme.documents.api.usecase;

import com.emme.documents.api.command.FailDocumentCommand;
import com.emme.documents.api.result.DocumentDetails;

public interface FailDocumentUseCase {
  DocumentDetails fail(FailDocumentCommand command);
}
