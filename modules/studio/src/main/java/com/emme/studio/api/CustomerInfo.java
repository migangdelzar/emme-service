package com.emme.studio.api;

import java.util.UUID;

public record CustomerInfo(UUID id, String name, String phone, String email) {}
