package com.emme.buildlogic

import com.emme.buildlogic.deployment.provider.KubernetesWorkload
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KubernetesWorkloadTest {
  @Test
  fun `uses the canonical backend deployment name`() {
    assertEquals("backend", KubernetesWorkload.DEPLOYMENT_NAME)
  }

  @Test
  fun `uses the canonical backend pod selector`() {
    assertEquals("app=emme-backend", KubernetesWorkload.POD_SELECTOR)
  }
}
