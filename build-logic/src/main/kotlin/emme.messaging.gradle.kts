import com.emme.buildlogic.core.dependency.EmmeDependencies
import org.gradle.api.artifacts.VersionCatalogsExtension

val libs = extensions.getByType(VersionCatalogsExtension::class).named("libs")
val e = EmmeDependencies(libs)

dependencies {
  add("implementation", e.springKafka)
  add("implementation", e.springModulithEventsKafka)
  add("testImplementation", e.testcontainersKafka)
}
