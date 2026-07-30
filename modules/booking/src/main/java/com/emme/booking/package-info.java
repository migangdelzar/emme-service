@org.springframework.modulith.ApplicationModule(
    displayName = "Booking",
    allowedDependencies = {
      "shared",
      "tenancy",
      "studio :: studio-api",
      "studio :: studio-events",
      "customer :: customer-api",
      "workforce :: workforce-api",
      "catalog :: catalog-api"
    })
package com.emme.booking;
