package com.emme.assistant.api.usecase;

import com.emme.assistant.api.command.ProposePendingActionCommand;
import com.emme.assistant.api.result.PendingActionInfo;

public interface ProposePendingActionUseCase {
  PendingActionInfo propose(ProposePendingActionCommand command);
}
