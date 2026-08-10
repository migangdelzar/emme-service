package com.emme.documents.api.usecase;

import com.emme.documents.api.command.UploadDocumentCommand;
import com.emme.documents.api.result.DocumentDetails;

public interface UploadDocumentUseCase {
  DocumentDetails upload(UploadDocumentCommand command);
}
