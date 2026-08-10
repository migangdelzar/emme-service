package com.emme.buildlogic

import com.emme.buildlogic.secrets.SecretProviderKind
import com.emme.buildlogic.secrets.provider.SecretProviderFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SecretsProviderTest {
  @Test
  fun `auto selection prefers actions over local providers`() {
    val selected =
      SecretProviderFactory.select(
        SecretProviderKind.AUTO,
        mapOf("GITHUB_ACTIONS" to "true", "BW_SESSION" to "present"),
      )

    assertThat(selected).isEqualTo(SecretProviderKind.GITHUB_ACTIONS)
  }

  @Test
  fun `auto selection uses bitwarden when a local session exists`() {
    val selected =
      SecretProviderFactory.select(
        SecretProviderKind.AUTO,
        mapOf("BW_SESSION" to "present"),
      )

    assertThat(selected).isEqualTo(SecretProviderKind.BITWARDEN)
  }

  @Test
  fun `explicit selection wins over auto detection`() {
    val selected =
      SecretProviderFactory.select(
        SecretProviderKind.ENVIRONMENT,
        mapOf("GITHUB_ACTIONS" to "true"),
      )

    assertThat(selected).isEqualTo(SecretProviderKind.ENVIRONMENT)
  }
}
