package com.emme.assistant.api.usecase;

import com.emme.assistant.api.command.RejectPendingActionCommand;
import com.emme.assistant.api.result.PendingActionDetails;

public interface RejectPendingActionUseCase {
  PendingActionDetails reject(RejectPendingActionCommand command);
}
