package com.emme.assistant.api.command;

import java.util.UUID;

public record RejectPendingActionCommand(UUID actionId) {}
