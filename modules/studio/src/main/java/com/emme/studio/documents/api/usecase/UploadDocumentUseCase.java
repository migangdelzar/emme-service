package com.emme.studio.documents.api.usecase;

import com.emme.studio.documents.api.command.UploadDocumentCommand;
import com.emme.studio.documents.api.result.DocumentInfo;

public interface UploadDocumentUseCase {
  DocumentInfo upload(UploadDocumentCommand command);
}
