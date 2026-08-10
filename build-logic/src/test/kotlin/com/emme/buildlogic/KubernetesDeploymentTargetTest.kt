package com.emme.buildlogic

import com.emme.buildlogic.deployment.KubernetesDeploymentTarget
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class KubernetesDeploymentTargetTest {
  @Test
  fun `normalizes local aliases to the k3d runtime overlay`() {
    assertThat(KubernetesDeploymentTarget.overlayName("local", "jvm"))
      .isEqualTo("k3d-jvm")
    assertThat(KubernetesDeploymentTarget.namespace("dev"))
      .isEqualTo("emme-dev")
  }

  @Test
  fun `normalizes production aliases to the k3s production runtime overlay`() {
    assertThat(KubernetesDeploymentTarget.overlayName("prod", "native"))
      .isEqualTo("k3s-production-native")
    assertThat(KubernetesDeploymentTarget.namespace("production"))
      .isEqualTo("emme-prod")
  }

  @Test
  fun `rejects unsupported profiles and runtimes`() {
    assertThrows<IllegalArgumentException> {
      KubernetesDeploymentTarget.overlayName("qa", "jvm")
    }
    assertThrows<IllegalArgumentException> {
      KubernetesDeploymentTarget.overlayName("local", "graal")
    }
  }
}
