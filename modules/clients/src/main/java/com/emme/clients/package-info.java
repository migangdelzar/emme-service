@org.springframework.modulith.ApplicationModule(
    displayName = "Clients",
    allowedDependencies = {"shared :: persistence", "tenancy", "subscriptions :: subscriptions-api"})
package com.emme.clients;
