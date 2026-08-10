plugins {
  `kotlin-dsl`
  `java-gradle-plugin`
}

group = "com.emme.buildlogic.settings"

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(25))
  }
}

gradlePlugin {
  plugins {
    register("environmentSettings") {
      id = "com.emme.environment-settings"
      implementationClass = "com.emme.buildlogic.settings.EnvironmentSettingsPlugin"
    }
  }
}
