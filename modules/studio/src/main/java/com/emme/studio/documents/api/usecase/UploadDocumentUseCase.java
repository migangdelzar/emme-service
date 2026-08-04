package com.emme.studio.documents.api.usecase;

import com.emme.studio.documents.api.command.UploadDocumentCommand;
import com.emme.studio.documents.api.result.DocumentDetails;

public interface UploadDocumentUseCase {
  DocumentDetails upload(UploadDocumentCommand command);
}
