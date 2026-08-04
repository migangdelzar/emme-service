package com.emme.studio.documents.api.usecase;

import com.emme.studio.documents.api.command.FailDocumentCommand;
import com.emme.studio.documents.api.result.DocumentDetails;

public interface FailDocumentUseCase {
  DocumentDetails fail(FailDocumentCommand command);
}
