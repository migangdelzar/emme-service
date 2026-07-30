package com.emme.buildlogic

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText

class SpringApplicationConventionFunctionalTest {

    @Test
    fun `applies spring-application convention and produces bootJar`(@TempDir projectDir: Path) {
        writeSettingsWithCatalog(projectDir)
        writePlatformProject(projectDir)

        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("emme.spring-application")
            }
            """.trimIndent()
        )

        projectDir.resolve("src/main/java/com/emme/app/App.java").apply {
            parent.toFile().mkdirs()
            writeText(
                """
                package com.emme.app;
                import org.springframework.boot.SpringApplication;
                import org.springframework.boot.autoconfigure.SpringBootApplication;
                @SpringBootApplication
                public class App {
                    public static void main(String[] args) {
                        SpringApplication.run(App.class, args);
                    }
                }
                """.trimIndent()
            )
        }

        val result = GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withArguments("bootJar", "-x", "checkstyleMain", "--stacktrace")
            .build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")
        assertThat(projectDir.resolve("build/libs/emme-studio.jar").toFile()).exists()
    }

    private fun writeSettingsWithCatalog(projectDir: Path) {
        val buildLogicPath = findBuildLogicDir()
        val tomlPath = findCatalogPath()
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement {
                includeBuild("${escapePath(buildLogicPath)}")
                repositories {
                    gradlePluginPortal()
                    mavenCentral()
                }
            }
            dependencyResolutionManagement {
                repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
                repositories {
                    mavenCentral()
                }
                versionCatalogs {
                    create("libs") {
                        from(files("${escapePath(tomlPath)}"))
                    }
                }
            }
            rootProject.name = "test-spring-app"
            include(":platform")
            """.trimIndent()
        )
    }

    private fun writePlatformProject(projectDir: Path) {
        projectDir.resolve("platform").toFile().mkdirs()
        projectDir.resolve("platform/build.gradle.kts").writeText(
            """
            plugins { `java-platform` }
            javaPlatform { allowDependencies() }
            dependencies {
                constraints {
                    api("org.springframework.boot:spring-boot-dependencies:4.1.0")
                }
            }
            """.trimIndent()
        )
    }
}
