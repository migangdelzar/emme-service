@org.springframework.modulith.ApplicationModule(
    displayName = "Identity & Security",
    allowedDependencies = {
      "shared",
      "tenancy",
      "tenancy :: tenant-api",
      "studio :: subscriptions-api",
      "studio :: studio-api"
    })
package com.emme.identity;
