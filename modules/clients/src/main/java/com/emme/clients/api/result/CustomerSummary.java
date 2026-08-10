package com.emme.clients.api.result;

import java.util.UUID;

public record CustomerSummary(UUID id, String name, String phone, String email) {}
