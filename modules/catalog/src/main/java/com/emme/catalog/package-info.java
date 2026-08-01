@org.springframework.modulith.ApplicationModule(
    displayName = "Catalog",
    allowedDependencies = {
      "shared",
      "shared :: search",
      "tenancy",
      "assistant :: assistant-ai-api"
    })
package com.emme.catalog;
