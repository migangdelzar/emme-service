package com.emme.documents.api.usecase;

import com.emme.documents.api.command.ChunkDocumentCommand;
import com.emme.documents.api.result.DocumentChunkDetails;
import java.util.List;

public interface ChunkDocumentUseCase {
  List<DocumentChunkDetails> chunk(ChunkDocumentCommand command);
}
