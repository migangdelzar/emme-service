plugins {
  `kotlin-dsl`
  `java-gradle-plugin`
  `jvm-test-suite`
  alias(libs.plugins.spotless)
  alias(libs.plugins.detekt)
}

group = "com.emme.buildlogic"

spotless {
  kotlin {
    target("src/**/*.kt")
    ktlint()
    trimTrailingWhitespace()
    endWithNewline()
  }
  kotlinGradle {
    target("*.gradle.kts", "src/**/*.gradle.kts")
    ktlint()
    trimTrailingWhitespace()
    endWithNewline()
  }
}

detekt {
  config.setFrom(files("config/detekt.yml"))
  baseline = file("detekt-baseline.xml")
  buildUponDefaultConfig = true
  parallel = true
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(25))
  }
}

dependencies {
  implementation(libs.spring.boot.gradle.plugin)
  implementation(libs.spotless.gradle.plugin)
  implementation(libs.detekt.gradle.plugin)
  implementation(libs.dependency.analysis.gradle.plugin)
  implementation(libs.gradle.versions.plugin)
  implementation(libs.jib.gradle.plugin)
  implementation(libs.cyclonedx.gradle.plugin)
  implementation(libs.dependency.check.gradle.plugin)
  implementation(libs.sonarqube.gradle.plugin)
  implementation(libs.japicmp.gradle.plugin)

  testImplementation(gradleTestKit())
  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.assertj.core)
}

testing {
  suites {
    val test by getting(JvmTestSuite::class) {
      useJUnitJupiter()
    }

    val functionalTest by registering(JvmTestSuite::class) {
      useJUnitJupiter()

      dependencies {
        implementation(project())
        implementation(gradleTestKit())
        implementation(libs.assertj.core)
      }

      targets {
        all {
          testTask.configure {
            shouldRunAfter(test)
          }
        }
      }
    }
  }
}

tasks.named("check") {
  dependsOn("spotlessCheck", "detekt")
  dependsOn(testing.suites.named("functionalTest"))
}

gradlePlugin {
  plugins {
    register("emmeRoot") {
      id = "com.emme.root"
      implementationClass = "com.emme.buildlogic.plugin.EmmeRootPlugin"
    }
    register("emmeContainerBinary") {
      id = "com.emme.container-binary"
      implementationClass = "com.emme.buildlogic.plugin.EmmeContainerPlugin"
    }
    register("emmePublishingBinary") {
      id = "com.emme.publishing-binary"
      implementationClass = "com.emme.buildlogic.plugin.EmmePublishingPlugin"
    }
    register("emmeDeployment") {
      id = "com.emme.deployment"
      implementationClass = "com.emme.buildlogic.plugin.EmmeDeploymentPlugin"
    }
  }
}
