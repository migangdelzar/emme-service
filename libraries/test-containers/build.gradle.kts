plugins {
    `java-library`
}

group = "com.emme"

// Internal module — never published
tasks.withType<PublishToMavenRepository>().configureEach { enabled = false }
tasks.withType<PublishToMavenLocal>().configureEach { enabled = false }

dependencies {
    implementation(platform(project(":platform")))
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-starter-test")
    implementation("org.springframework.boot:spring-boot-test-autoconfigure")
    implementation("org.springframework.boot:spring-boot-testcontainers")
    implementation("org.testcontainers:postgresql")
    implementation("org.testcontainers:junit-jupiter")
    implementation("org.testcontainers:testcontainers")
}
