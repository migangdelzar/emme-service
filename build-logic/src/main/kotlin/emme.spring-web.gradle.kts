import com.emme.buildlogic.core.dependency.Dependencies
import org.gradle.api.artifacts.VersionCatalogsExtension

val libs = extensions.getByType(VersionCatalogsExtension::class).named("libs")
val e = Dependencies(libs)

dependencies {
  add("implementation", e.springWebmvc)
  add("implementation", e.springBootStarterValidation)
}
