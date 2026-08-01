package com.emme.assistant.api.usecase;

import com.emme.assistant.api.command.ConfirmPendingActionCommand;
import com.emme.assistant.api.result.PendingActionInfo;

public interface ConfirmPendingActionUseCase {
  PendingActionInfo confirm(ConfirmPendingActionCommand command);
}
