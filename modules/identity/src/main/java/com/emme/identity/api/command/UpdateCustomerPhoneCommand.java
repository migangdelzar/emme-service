package com.emme.identity.api.command;

import java.util.UUID;

/** Request to update the authenticated customer's phone number. */
public record UpdateCustomerPhoneCommand(UUID customerId, String phone) {}
