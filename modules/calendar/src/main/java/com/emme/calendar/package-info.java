@org.springframework.modulith.ApplicationModule(
    displayName = "Calendar",
    allowedDependencies = {
      "shared :: persistence",
      "shared :: web-security",
      "tenancy",
      "tenancy :: tenant-api",
      "identity :: identity-security",
      "appointments :: appointments-api",
      "appointments :: appointments-events",
      "clients :: clients-api"
    })
package com.emme.calendar;
