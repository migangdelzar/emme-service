@org.springframework.modulith.ApplicationModule(
    displayName = "Calendar",
    allowedDependencies = {
      "shared :: persistence",
      "tenancy",
      "identity :: identity-security",
      "studio",
      "studio :: studio-api",
      "studio :: studio-events"
    })
package com.emme.calendar;
