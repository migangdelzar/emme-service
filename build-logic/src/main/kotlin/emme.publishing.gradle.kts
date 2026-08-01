apply(plugin = "com.emme.publishing-binary")

// CycloneDX SBOM generation for supply chain security
pluginManager.withPlugin("org.cyclonedx.bom") {
  tasks.withType<org.cyclonedx.gradle.CycloneDxTask>().configureEach {
    outputFormat.set("json")
    includeBomSerialNumber.set(true)
    includeLicenseText.set(true)
  }
}
