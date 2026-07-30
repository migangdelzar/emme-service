@org.springframework.modulith.ApplicationModule(
    displayName = "Studio Domain",
    allowedDependencies = {"shared", "tenancy", "notification :: notification-events", "payment"})
package com.emme.studio;
