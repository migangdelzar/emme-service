package com.emme.assistant.application.port.out;

import java.util.UUID;

/** Resolves a provider account identifier to an authorized tenant. */
public interface WhatsAppTenantResolver {
  UUID resolve(String providerAccountId);
}
