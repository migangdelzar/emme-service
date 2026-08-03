import com.emme.buildlogic.core.dependency.EmmeDependencies
import org.gradle.api.artifacts.VersionCatalogsExtension

val libs = extensions.getByType(VersionCatalogsExtension::class).named("libs")
val e = EmmeDependencies(libs)

dependencies {
  add("implementation", e.springWebmvc)
  add("implementation", e.springBootStarterValidation)
}
