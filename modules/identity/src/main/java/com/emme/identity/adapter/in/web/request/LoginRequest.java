package com.emme.identity.adapter.in.web.request;

/** Credentials submitted to the staff login endpoint. */
public record LoginRequest(String email, String password) {}
