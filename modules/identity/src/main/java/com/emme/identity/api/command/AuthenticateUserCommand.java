package com.emme.identity.api.command;

/** Request to authenticate a staff or platform user with username and password. */
public record AuthenticateUserCommand(String username, String password) {}
