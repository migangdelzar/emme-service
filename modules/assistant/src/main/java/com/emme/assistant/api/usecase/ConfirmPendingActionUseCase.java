package com.emme.assistant.api.usecase;

import com.emme.assistant.api.command.ConfirmPendingActionCommand;
import com.emme.assistant.api.result.PendingActionDetails;

public interface ConfirmPendingActionUseCase {
  PendingActionDetails confirm(ConfirmPendingActionCommand command);
}
