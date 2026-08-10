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
  implementation(libs.graalvm.native.gradle.plugin)
  implementation(libs.jackson.databind)

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
      implementationClass = "com.emme.buildlogic.root.RootPlugin"
    }
    register("emmeContainerBinary") {
      id = "com.emme.container-binary"
      implementationClass = "com.emme.buildlogic.container.ContainerPlugin"
    }
    register("emmePublishingBinary") {
      id = "com.emme.publishing-binary"
      implementationClass = "com.emme.buildlogic.publishing.PublishingPlugin"
    }
    register("emmeDeployment") {
      id = "com.emme.deployment"
      implementationClass = "com.emme.buildlogic.deployment.DeploymentPlugin"
    }
    register("emmeEnvironment") {
      id = "com.emme.environment"
      implementationClass = "com.emme.buildlogic.environment.EnvironmentPlugin"
    }
    register("emmeSecrets") {
      id = "com.emme.secrets"
      implementationClass = "com.emme.buildlogic.secrets.SecretsPlugin"
    }
    register("emmeGoogleIdentity") {
      id = "emme.google-identity"
      implementationClass = "com.emme.buildlogic.google.GoogleIdentityTasksPlugin"
    }
    register("emmeSecurityBinary") {
      id = "com.emme.security-binary"
      implementationClass = "com.emme.buildlogic.security.SecurityPlugin"
    }
  }
}
