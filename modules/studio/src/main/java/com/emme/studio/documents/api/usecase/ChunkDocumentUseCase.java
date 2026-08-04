package com.emme.studio.documents.api.usecase;

import com.emme.studio.documents.api.command.ChunkDocumentCommand;
import com.emme.studio.documents.api.result.DocumentChunkDetails;
import java.util.List;

public interface ChunkDocumentUseCase {
  List<DocumentChunkDetails> chunk(ChunkDocumentCommand command);
}
