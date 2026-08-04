package com.emme.studio.documents.api.usecase;

import com.emme.studio.documents.api.command.ProcessDocumentCommand;
import com.emme.studio.documents.api.result.DocumentDetails;

public interface ProcessDocumentUseCase {
  DocumentDetails process(ProcessDocumentCommand command);
}
