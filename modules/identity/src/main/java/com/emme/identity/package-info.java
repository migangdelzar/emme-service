@org.springframework.modulith.ApplicationModule(
    displayName = "Identity & Security",
    allowedDependencies = {
      "shared :: persistence",
      "tenancy",
      "tenancy :: tenant-api",
      "tenancy :: tenant-events",
      "studio :: subscriptions-api",
      "studio :: studio-api",
      "studio :: studio-events"
    })
package com.emme.identity;
