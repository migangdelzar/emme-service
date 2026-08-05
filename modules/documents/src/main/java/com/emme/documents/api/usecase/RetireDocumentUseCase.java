package com.emme.documents.api.usecase;

import com.emme.documents.api.command.RetireDocumentCommand;
import com.emme.documents.api.result.DocumentDetails;

public interface RetireDocumentUseCase {
  DocumentDetails retire(RetireDocumentCommand command);
}
