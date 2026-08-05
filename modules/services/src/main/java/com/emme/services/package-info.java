@org.springframework.modulith.ApplicationModule(
    displayName = "Services",
    allowedDependencies = {
      "shared :: persistence", "tenancy", "subscriptions :: subscriptions-api"
    })
package com.emme.services;
