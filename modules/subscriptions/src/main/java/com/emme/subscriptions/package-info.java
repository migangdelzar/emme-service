@org.springframework.modulith.ApplicationModule(
    displayName = "Subscriptions",
    allowedDependencies = {
      "shared :: persistence",
      "tenancy",
      "tenancy :: tenant-api",
      "tenancy :: tenant-events"
    })
package com.emme.subscriptions;
