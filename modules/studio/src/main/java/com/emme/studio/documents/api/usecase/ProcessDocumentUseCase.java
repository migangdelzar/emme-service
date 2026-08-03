package com.emme.studio.documents.api.usecase;

import com.emme.studio.documents.api.command.ProcessDocumentCommand;
import com.emme.studio.documents.api.result.DocumentInfo;

public interface ProcessDocumentUseCase {
  DocumentInfo process(ProcessDocumentCommand command);
}
