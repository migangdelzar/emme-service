package com.emme.buildlogic.internal

import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

object TestConfiguration {
  fun apply(project: Project) {
    project.tasks.withType(Test::class.java).configureEach {
      useJUnitPlatform()

      jvmArgs("--enable-preview")

      reports {
        junitXml.required.set(true)
        html.required.set(true)
      }

      testLogging {
        events("failed", "skipped")
        exceptionFormat = TestExceptionFormat.FULL
      }
    }
  }
}
