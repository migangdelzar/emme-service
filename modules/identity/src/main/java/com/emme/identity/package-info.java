@org.springframework.modulith.ApplicationModule(
    displayName = "Identity & Security",
    allowedDependencies = {
      "shared :: persistence",
      "tenancy",
      "tenancy :: tenant-api",
      "tenancy :: tenant-events",
      "appointments :: appointments-api",
      "appointments :: appointments-events",
      "salon :: salon-api",
      "subscriptions :: subscriptions-api"
    })
package com.emme.identity;
