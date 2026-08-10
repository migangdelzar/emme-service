package com.emme.assistant.api.command;

import java.util.UUID;

public record ConfirmPendingActionCommand(UUID tenantId, UUID actionId) {}
