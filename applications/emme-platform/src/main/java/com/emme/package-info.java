@org.springframework.modulith.ApplicationModule(
    displayName = "Application Root",
    allowedDependencies = {
      "shared",
      "tenancy",
      "identity",
      "studio",
      "customer",
      "workforce",
      "catalog",
      "booking",
      "calendar",
      "notification",
      "payment",
      "assistant",
      "audit"
    })
package com.emme;
