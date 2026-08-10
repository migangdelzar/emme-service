package com.emme.identity.adapter.in.web.response;

/** HTTP representation of a customer profile returned by customer login. */
public record CustomerProfileResponse(
    String id, String email, String name, String phone, String provider) {}
