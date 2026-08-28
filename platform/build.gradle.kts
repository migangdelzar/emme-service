plugins {
    `java-platform`
}

group = "com.emme.platform"

javaPlatform {
    allowDependencies()
}

dependencies {
    // Spring Boot BOM - manages all spring-boot-starter-* versions
    api(platform(libs.spring.boot.bom))

    // Spring Modulith BOM - manages spring-modulith-* versions
    api(platform(libs.spring.modulith.bom))

    // AI BOMs are centrally pinned; modules opt into concrete integrations.
    api(platform(libs.spring.ai.bom))
    api(platform(libs.langgraph4j.bom))

    // Testcontainers BOM - manages testcontainers-* versions
    api(platform(libs.testcontainers.bom))

    // JUnit BOM - manages junit-* versions
    api(platform(libs.junit.bom))

    constraints {
        // Database
        api(libs.postgresql)
        api(libs.h2)
        api(libs.liquibase.core)
        api(libs.assertj.core)
        api(libs.mockito.core)
        api(libs.awaitility)

        // ShedLock
        api(libs.shedlock.spring)
        api(libs.shedlock.provider.jdbc.template)

        // OpenAPI
        api(libs.springdoc.openapi.starter.webmvc.ui)

        // Utilities
        api(libs.java.uuid.generator)
        api(libs.okhttp)
        api(libs.okhttp.logging.interceptor)
        api(libs.okhttp.mockwebserver)

        // ArchUnit
        api(libs.archunit.junit5)

        // Testcontainers
        api(libs.testcontainers)
        api(libs.testcontainers.postgresql)
        api(libs.testcontainers.kafka)
        api(libs.testcontainers.junit.jupiter)

        // Docker Java (for Testcontainers compatibility with Docker 29.x)
        api(libs.docker.java.api)
        api(libs.docker.java.transport.zerodep)

        // Email testing
        api(libs.greenmail)
    }
}
