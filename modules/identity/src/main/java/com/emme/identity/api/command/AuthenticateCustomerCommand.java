package com.emme.identity.api.command;

/** Request to authenticate a customer with a provider-issued token. */
public record AuthenticateCustomerCommand(String providerToken) {}
