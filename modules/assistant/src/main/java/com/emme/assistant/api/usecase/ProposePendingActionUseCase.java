package com.emme.assistant.api.usecase;

import com.emme.assistant.api.command.ProposePendingActionCommand;
import com.emme.assistant.api.result.PendingActionDetails;

public interface ProposePendingActionUseCase {
  PendingActionDetails propose(ProposePendingActionCommand command);
}
