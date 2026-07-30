package com.emme.buildlogic

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText

class IntegrationTestingConventionFunctionalTest {

    @Test
    fun `creates integrationTest task`(@TempDir projectDir: Path) {
        writeSettingsWithCatalog(projectDir)
        writePlatformProject(projectDir)

        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("emme.java-library")
                id("emme.integration-testing")
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withArguments("tasks", "--group=verification", "--stacktrace")
            .build()

        assertThat(result.output).contains("integrationTest")
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
            rootProject.name = "test-integration"
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
