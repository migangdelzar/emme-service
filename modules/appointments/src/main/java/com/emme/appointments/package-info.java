@org.springframework.modulith.ApplicationModule(
    displayName = "Appointments",
    allowedDependencies = {
      "shared :: persistence",
      "tenancy",
      "services",
      "services :: services-api",
      "clients",
      "clients :: clients-api",
      "salon",
      "salon :: salon-api",
      "subscriptions :: subscriptions-api",
      "notification :: notification-events"
    })
package com.emme.appointments;
