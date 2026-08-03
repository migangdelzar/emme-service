package com.emme.studio.api.result;

import java.util.UUID;

public record CustomerInfo(UUID id, String name, String phone, String email) {}
