package com.emme.studio.documents.api.usecase;

import com.emme.studio.documents.api.command.FailDocumentCommand;
import com.emme.studio.documents.api.result.DocumentInfo;

public interface FailDocumentUseCase {
  DocumentInfo fail(FailDocumentCommand command);
}
