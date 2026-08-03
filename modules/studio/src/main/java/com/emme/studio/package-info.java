@org.springframework.modulith.ApplicationModule(
    displayName = "Studio Domain",
    allowedDependencies = {
      "shared :: persistence",
      "shared :: search",
      "tenancy",
      "notification :: notification-events",
      "payment"
    })
package com.emme.studio;
