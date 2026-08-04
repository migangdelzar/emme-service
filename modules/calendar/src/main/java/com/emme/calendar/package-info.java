@org.springframework.modulith.ApplicationModule(
    displayName = "Calendar",
    allowedDependencies = {
      "shared :: persistence",
      "shared :: web-security",
      "tenancy",
      "identity :: identity-security",
      "studio",
      "studio :: studio-api",
      "studio :: studio-events"
    })
package com.emme.calendar;
