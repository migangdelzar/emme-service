import com.emme.buildlogic.core.dependency.Dependencies
import org.gradle.api.artifacts.VersionCatalogsExtension

val libs = extensions.getByType(VersionCatalogsExtension::class).named("libs")
val e = Dependencies(libs)

dependencies {
  add("implementation", e.springKafka)
  add("implementation", e.springModulithEventsKafka)
  add("testImplementation", e.testcontainersKafka)
}
