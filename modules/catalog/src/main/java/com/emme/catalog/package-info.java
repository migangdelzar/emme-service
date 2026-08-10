@org.springframework.modulith.ApplicationModule(
    displayName = "Catalog",
    allowedDependencies = {
      "shared :: persistence",
      "shared :: identity",
      "shared :: search",
      "tenancy",
      "assistant :: assistant-ai-api"
    })
package com.emme.catalog;
