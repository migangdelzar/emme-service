package com.emme.buildlogic

import com.emme.buildlogic.deployment.DeploymentTarget
import com.emme.buildlogic.quality.QualityGateMode
import com.emme.buildlogic.registry.RegistryTarget
import com.emme.buildlogic.security.SecurityScanner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ModelEnumTest {
  @Test
  fun `DeploymentTarget fromString case insensitive`() {
    assertEquals(DeploymentTarget.COMPOSE, DeploymentTarget.fromString("compose"))
    assertEquals(DeploymentTarget.COMPOSE, DeploymentTarget.fromString("COMPOSE"))
    assertEquals(DeploymentTarget.KUBERNETES, DeploymentTarget.fromString("kubernetes"))
    assertEquals(DeploymentTarget.COMPOSE, DeploymentTarget.fromString("unknown"))
  }

  @Test
  fun `RegistryTarget fromString case insensitive`() {
    assertEquals(RegistryTarget.LOCAL, RegistryTarget.fromString("local"))
    assertEquals(RegistryTarget.GHCR, RegistryTarget.fromString("GHCR"))
    assertEquals(RegistryTarget.LOCAL, RegistryTarget.fromString("unknown"))
  }

  @Test
  fun `SecurityScanner fromString case insensitive`() {
    assertEquals(SecurityScanner.TRIVY, SecurityScanner.fromString("trivy"))
    assertEquals(SecurityScanner.GRYPE, SecurityScanner.fromString("GRYPE"))
    assertEquals(SecurityScanner.TRIVY, SecurityScanner.fromString("unknown"))
  }

  @Test
  fun `QualityGateMode fromString case insensitive`() {
    assertEquals(QualityGateMode.STRICT, QualityGateMode.fromString("strict"))
    assertEquals(QualityGateMode.WARN, QualityGateMode.fromString("WARN"))
    assertEquals(QualityGateMode.STRICT, QualityGateMode.fromString("unknown"))
  }
}
