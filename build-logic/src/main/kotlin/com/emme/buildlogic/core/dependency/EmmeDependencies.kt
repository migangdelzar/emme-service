package com.emme.buildlogic.core.dependency

import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

// Typed wrapper around the root VersionCatalog so precompiled convention
// plugins have IDE-autocompletable dependency accessors.
//
// Usage: val e = EmmeDependencies(catalog)
//   implementation(e.springWebmvc)
//   testImplementation(e.assertjCore)
//
// In root build.gradle.kts, the auto-generated libs accessor works directly:
//   implementation(libs.spring.webmvc)
class EmmeDependencies(
  private val catalog: VersionCatalog,
) {
  private fun lib(name: String): Provider<MinimalExternalModuleDependency> = catalog.findLibrary(name).get()

  // ---- Spring Boot ----
  val springContext get() = lib("spring-context")
  val springWeb get() = lib("spring-web")
  val springWebmvc get() = lib("spring-webmvc")
  val springJdbc get() = lib("spring-jdbc")
  val springKafka get() = lib("spring-kafka")
  val springBootStarter get() = lib("spring-boot-starter")
  val springBootStarterWeb get() = lib("spring-boot-starter-web")
  val springBootStarterSecurity get() = lib("spring-boot-starter-security")
  val springBootStarterDataJpa get() = lib("spring-boot-starter-data-jpa")
  val springBootStarterDataRedis get() = lib("spring-boot-starter-data-redis")
  val springBootStarterActuator get() = lib("spring-boot-starter-actuator")
  val springBootStarterValidation get() = lib("spring-boot-starter-validation")
  val springBootStarterOauth2ResourceServer get() = lib("spring-boot-starter-oauth2-resource-server")
  val springBootStarterOauth2Client get() = lib("spring-boot-starter-oauth2-client")
  val springBootStarterAop get() = lib("spring-boot-starter-aop")
  val springBootStarterMail get() = lib("spring-boot-starter-mail")
  val springBootDevtools get() = lib("spring-boot-devtools")
  val springBootConfigurationProcessor get() = lib("spring-boot-configuration-processor")
  val springBootStarterTest get() = lib("spring-boot-starter-test")
  val springBootResttestclient get() = lib("spring-boot-resttestclient")
  val springBootWebmvcTest get() = lib("spring-boot-webmvc-test")
  val springBootRestclient get() = lib("spring-boot-restclient")
  val springBootDataJpaTest get() = lib("spring-boot-data-jpa-test")

  // ---- Spring Security ----
  val springSecurityCore get() = lib("spring-security-core")
  val springSecurityTest get() = lib("spring-security-test")

  // ---- Spring Modulith ----
  val springModulithApi get() = lib("spring-modulith-api")
  val springModulithStarterCore get() = lib("spring-modulith-starter-core")
  val springModulithStarterJpa get() = lib("spring-modulith-starter-jpa")
  val springModulithStarterJdbc get() = lib("spring-modulith-starter-jdbc")
  val springModulithStarterTest get() = lib("spring-modulith-starter-test")
  val springModulithEventsCore get() = lib("spring-modulith-events-core")
  val springModulithEventsKafka get() = lib("spring-modulith-events-kafka")
  val springModulithObservability get() = lib("spring-modulith-observability")
  val springModulithCore get() = lib("spring-modulith-core")
  val springModulithDocs get() = lib("spring-modulith-docs")
  val springModulithActuator get() = lib("spring-modulith-actuator")

  // ---- Database ----
  val postgresql get() = lib("postgresql")
  val h2 get() = lib("h2")
  val liquibaseCore get() = lib("liquibase-core")

  // ---- Observability ----
  val micrometerTracingBridgeOtel get() = lib("micrometer-tracing-bridge-otel")
  val micrometerRegistryPrometheus get() = lib("micrometer-registry-prometheus")

  // ---- Testing ----
  val junitJupiter get() = lib("junit-jupiter")
  val junitPlatformLauncher get() = lib("junit-platform-launcher")
  val assertjCore get() = lib("assertj-core")
  val mockitoCore get() = lib("mockito-core")
  val mockitoJunitJupiter get() = lib("mockito-junit-jupiter")
  val archunitJunit5 get() = lib("archunit-junit5")
  val awaitility get() = lib("awaitility")
  val greenmail get() = lib("greenmail")
  val testcontainers get() = lib("testcontainers")
  val testcontainersPostgresql get() = lib("testcontainers-postgresql")
  val testcontainersKafka get() = lib("testcontainers-kafka")
  val testcontainersJunitJupiter get() = lib("testcontainers-junit-jupiter")
  val dockerJavaApi get() = lib("docker-java-api")
  val dockerJavaTransportZerodep get() = lib("docker-java-transport-zerodep")

  // ---- Utilities ----
  val caffeine get() = lib("caffeine")
  val okhttp get() = lib("okhttp")
  val okhttpLoggingInterceptor get() = lib("okhttp-logging-interceptor")
  val okhttpMockwebserver get() = lib("okhttp-mockwebserver")
  val jacksonDatabind get() = lib("jackson-databind")
  val javaUuidGenerator get() = lib("java-uuid-generator")
  val shedlockSpring get() = lib("shedlock-spring")
  val shedlockProviderJdbcTemplate get() = lib("shedlock-provider-jdbc-template")
  val springdocOpenapiStarterWebmvcUi get() = lib("springdoc-openapi-starter-webmvc-ui")
}
