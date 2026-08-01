package com.emme.assistant.api.usecase;

import com.emme.assistant.api.command.RejectPendingActionCommand;
import com.emme.assistant.api.result.PendingActionInfo;

public interface RejectPendingActionUseCase {
  PendingActionInfo reject(RejectPendingActionCommand command);
}
