plugins {
  java
  id("emme.quality")
  checkstyle
}

dependencyLocking {
  lockAllConfigurations()
  // Locking provides reproducibility; failOnDynamicVersions/failOnChangingVersions
  // are incompatible with locking and would cause a build error.
}

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(25)
  }
  withSourcesJar()
}

// Input normalization: ignore volatile build info files for better cache hits
normalization {
  runtimeClasspath {
    ignore("build-info.properties")
    ignore("META-INF/build-info.properties")
  }
}

tasks.withType<JavaCompile>().configureEach {
  options.encoding = "UTF-8"
  options.release.set(25)
  // Enable incremental compilation for faster rebuilds
  options.isIncremental = true
  options.compilerArgs.addAll(
    listOf(
      "--enable-preview",
      "-parameters",
      "-Xlint:all",
      "-Xlint:-processing",
    ),
  )
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
  jvmArgs("--enable-preview")

  // Parallel execution — defaults to half the available processors, overridable via property
  maxParallelForks =
    providers
      .gradleProperty("emme.test.forks")
      .orElse(providers.environmentVariable("EMME_TEST_FORKS"))
      .map { it.toInt() }
      .orElse((Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1))
      .get()

  // Fork JVM after N tests to reduce memory pressure and isolate leaks
  forkEvery = 100

  reports {
    junitXml.required.set(true)
    html.required.set(true)
  }
  testLogging {
    events("failed", "skipped")
    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
  }
}

tasks.withType<JavaExec>().configureEach {
  jvmArgs("--enable-preview")
}

tasks.withType<Jar>().configureEach {
  isPreserveFileTimestamps = false
  isReproducibleFileOrder = true
  archiveClassifier = ""
}

checkstyle {
  toolVersion = "10.21.4"
  configFile = rootProject.file("build-logic/config/checkstyle.xml")
}
