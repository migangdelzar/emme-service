package com.emme.documents.api.usecase;

import com.emme.documents.api.command.ProcessDocumentCommand;
import com.emme.documents.api.result.DocumentDetails;

public interface ProcessDocumentUseCase {
  DocumentDetails process(ProcessDocumentCommand command);
}
