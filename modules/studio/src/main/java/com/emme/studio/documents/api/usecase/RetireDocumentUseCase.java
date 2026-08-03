package com.emme.studio.documents.api.usecase;

import com.emme.studio.documents.api.command.RetireDocumentCommand;
import com.emme.studio.documents.api.result.DocumentInfo;

public interface RetireDocumentUseCase {
  DocumentInfo retire(RetireDocumentCommand command);
}
