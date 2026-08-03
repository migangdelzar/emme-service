package com.emme.studio.adapter.in.web.request;

/** HTTP request for updating a Studio customer profile. */
public record UpdateCustomerRequest(String name, String phone, String email) {}
