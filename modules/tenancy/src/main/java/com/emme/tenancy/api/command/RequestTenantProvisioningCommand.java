package com.emme.tenancy.api.command;

public record RequestTenantProvisioningCommand(
    String slug, String name, String timeZone, String locale) {}
