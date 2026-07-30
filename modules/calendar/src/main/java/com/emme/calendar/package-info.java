@org.springframework.modulith.ApplicationModule(
    displayName = "Calendar",
    allowedDependencies = {
      "shared",
      "tenancy",
      "identity",
      "studio",
      "studio :: studio-api",
      "studio :: studio-events"
    })
package com.emme.calendar;
