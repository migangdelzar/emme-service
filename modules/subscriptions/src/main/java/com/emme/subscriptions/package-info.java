@org.springframework.modulith.ApplicationModule(
    displayName = "Subscriptions",
    allowedDependencies = {"shared :: persistence", "tenancy", "tenancy :: tenant-events"})
package com.emme.subscriptions;
