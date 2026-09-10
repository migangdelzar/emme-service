import com.emme.buildlogic.core.dependency.Dependencies
import org.gradle.api.artifacts.VersionCatalogsExtension

val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
val catalogDependencies = Dependencies(catalog)

dependencies {
  add("compileOnly", catalogDependencies.lombok)
  add("annotationProcessor", catalogDependencies.lombok)
  add("testCompileOnly", catalogDependencies.lombok)
  add("testAnnotationProcessor", catalogDependencies.lombok)
}
