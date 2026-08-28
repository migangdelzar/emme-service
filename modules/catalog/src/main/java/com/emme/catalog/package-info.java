@org.springframework.modulith.ApplicationModule(
    displayName = "Catalog",
    allowedDependencies = {
      "shared :: persistence",
      "shared :: identity",
      "shared :: search",
      "tenancy"
    })
package com.emme.catalog;
