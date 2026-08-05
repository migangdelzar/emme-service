@org.springframework.modulith.ApplicationModule(
    displayName = "Clients",
    allowedDependencies = {"shared :: persistence", "tenancy", "subscriptions :: subscriptions-api"})
@org.springframework.modulith.NamedInterface("clients-api")
package com.emme.clients;
