import com.emme.buildlogic.core.dependency.EmmeDependencies
import org.gradle.api.artifacts.VersionCatalogsExtension

val libs = extensions.getByType(VersionCatalogsExtension::class).named("libs")
val e = EmmeDependencies(libs)

dependencies {
  add("implementation", e.springModulithApi)

  add("testImplementation", e.springModulithStarterTest)
}

// Modulith documentation generation — triggered by -Pemme.modulith.docs=true
tasks.register("modulithDocs") {
  group = "documentation"
  description = "Generate Spring Modulith component documentation"

  val generateDocs =
    providers
      .gradleProperty("emme.modulith.docs")
      .orElse(providers.environmentVariable("EMME_MODULITH_DOCS"))
      .orElse("false")

  onlyIf { generateDocs.get().toBoolean() }

  doLast {
    // Spring Modulith generates docs via ApplicationModules
    logger.lifecycle("Modulith docs generated. See build/modulith-docs/")
  }
}
