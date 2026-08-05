@org.springframework.modulith.ApplicationModule(
    displayName = "Services",
    allowedDependencies = {"shared :: persistence", "tenancy", "subscriptions :: subscriptions-api"})
@org.springframework.modulith.NamedInterface("services-api")
package com.emme.services;
