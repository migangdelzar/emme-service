@org.springframework.modulith.ApplicationModule(
    displayName = "Assistant",
    allowedDependencies = {"shared :: persistence", "tenancy", "documents :: documents-api"})
package com.emme.assistant;
