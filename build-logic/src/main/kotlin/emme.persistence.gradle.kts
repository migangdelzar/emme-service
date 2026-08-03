import com.emme.buildlogic.core.dependency.EmmeDependencies
import org.gradle.api.artifacts.VersionCatalogsExtension

val libs = extensions.getByType(VersionCatalogsExtension::class).named("libs")
val e = EmmeDependencies(libs)

dependencies {
  add("implementation", e.springBootStarterDataJpa)
  add("implementation", e.liquibaseCore)

  add("runtimeOnly", e.postgresql)

  add("testImplementation", e.testcontainersJunitJupiter)
  add("testImplementation", e.testcontainersPostgresql)
}
