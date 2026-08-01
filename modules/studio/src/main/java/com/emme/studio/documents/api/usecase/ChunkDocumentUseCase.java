package com.emme.studio.documents.api.usecase;

import com.emme.studio.documents.api.command.ChunkDocumentCommand;
import com.emme.studio.documents.api.result.DocumentChunkInfo;
import java.util.List;

public interface ChunkDocumentUseCase {
  List<DocumentChunkInfo> chunk(ChunkDocumentCommand command);
}
