package com.emme.buildlogic.internal

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion

object JavaConfiguration {

    private const val JAVA_VERSION = 25

    fun apply(project: Project) {
        project.extensions.configure<JavaPluginExtension>("java") {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(JAVA_VERSION))
            }
            withSourcesJar()
        }

        project.tasks.withType(JavaCompile::class.java).configureEach {
            options.encoding = "UTF-8"
            options.release.set(JAVA_VERSION)
            options.compilerArgs.addAll(
                listOf(
                    "--enable-preview",
                    "-parameters",
                    "-Xlint:all",
                    "-Xlint:-processing",
                )
            )
        }
    }
}
