@org.springframework.modulith.ApplicationModule(
    displayName = "Salon",
    allowedDependencies = {"shared :: persistence", "tenancy"})
@org.springframework.modulith.NamedInterface("salon-api")
package com.emme.salon;
