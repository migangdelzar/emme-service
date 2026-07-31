package com.emme.buildlogic

import com.emme.buildlogic.container.provider.ContainerRuntimeProvider
import com.emme.buildlogic.container.provider.DockerProvider
import com.emme.buildlogic.deployment.provider.ComposeProvider
import com.emme.buildlogic.deployment.provider.DeploymentProvider
import com.emme.buildlogic.security.provider.SecurityScannerProvider
import com.emme.buildlogic.security.provider.TrivyProvider
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProviderRegistrationTest {
  @Test
  fun `DockerProvider extends ContainerRuntimeProvider`() {
    assertTrue(ContainerRuntimeProvider::class.java.isAssignableFrom(DockerProvider::class.java))
  }

  @Test
  fun `ComposeProvider extends DeploymentProvider`() {
    assertTrue(DeploymentProvider::class.java.isAssignableFrom(ComposeProvider::class.java))
  }

  @Test
  fun `TrivyProvider extends SecurityScannerProvider`() {
    assertTrue(SecurityScannerProvider::class.java.isAssignableFrom(TrivyProvider::class.java))
  }

  @Test
  fun `ContainerRuntimeProvider implements AutoCloseable`() {
    assertTrue(AutoCloseable::class.java.isAssignableFrom(ContainerRuntimeProvider::class.java))
  }

  @Test
  fun `DeploymentProvider implements AutoCloseable`() {
    assertTrue(AutoCloseable::class.java.isAssignableFrom(DeploymentProvider::class.java))
  }
}
