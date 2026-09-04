@org.springframework.modulith.ApplicationModule(
    displayName = "Assistant",
    allowedDependencies = {
      "shared :: persistence",
      "tenancy :: tenant-database",
      "tenancy :: tenant-api",
      "documents :: documents-api",
      "services :: services-api",
      "appointments :: appointments-api"
    })
package com.emme.assistant;
