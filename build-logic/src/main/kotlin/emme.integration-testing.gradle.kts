import com.emme.buildlogic.dependency.EmmeDependencies
import org.gradle.api.artifacts.VersionCatalogsExtension

val libs = extensions.getByType(VersionCatalogsExtension::class).named("libs")
val e = EmmeDependencies(libs)

plugins {
    `jvm-test-suite`
}

testing {
    suites {
        register<JvmTestSuite>("integrationTest") {
            useJUnitJupiter()

            dependencies {
                implementation(project.dependencies.create(project))
                implementation(project.dependencies.platform(
                    project.dependencies.project(mapOf("path" to ":platform"))
                ))
                // Generic test infrastructure (containers, annotations)
                implementation(project.dependencies.project(mapOf("path" to ":libraries:test-containers")))
                implementation(project.dependencies.project(mapOf("path" to ":libraries:functional")))
                // Framework dependencies (canonical supplier — do NOT rely on transitives)
                implementation(e.springBootStarterTest)
                implementation("org.springframework.boot:spring-boot-testcontainers")
                implementation(e.testcontainersJunitJupiter)
                implementation(e.testcontainersPostgresql)
                implementation(e.awaitility)
            }

            targets {
                all {
                    testTask.configure {
                        shouldRunAfter(testing.suites.named("test"))
                        environment("TESTCONTAINERS_RYUK_DISABLED", "true")
                        forkEvery = 100
                        jvmArgs("--enable-preview")
                    }
                }
            }
        }
    }
}

// Integration tests run explicitly: ./gradlew integrationTest
// CI pipeline triggers them separately.
