package com.emme.buildlogic

import com.emme.buildlogic.environment.EnvironmentName
import com.emme.buildlogic.environment.RuntimeKind
import com.emme.buildlogic.secrets.generator.SecureSecretGenerator
import com.emme.buildlogic.secrets.model.SecretRotationMode
import com.emme.buildlogic.secrets.model.SecretRotationRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class EnvironmentModelTest {
  @Test
  fun `accepts only the canonical environment names`() {
    assertThat(EnvironmentName.parse("production")).isEqualTo(EnvironmentName.PRODUCTION)
    assertThat(EnvironmentName.entries.map { it.id })
      .containsExactly("local", "dev", "regression", "staging", "production")
  }

  @Test
  fun `rejects unsupported environment names`() {
    assertThrows<IllegalArgumentException> { EnvironmentName.parse("prod") }
  }

  @Test
  fun `accepts only supported runtime artifacts`() {
    assertThat(RuntimeKind.parse("native")).isEqualTo(RuntimeKind.NATIVE)
    assertThrows<IllegalArgumentException> { RuntimeKind.parse("graal") }
  }

  @Test
  fun `generates opaque secrets with the requested length`() {
    val value = SecureSecretGenerator().generate(48)

    assertThat(value).hasSize(48)
    assertThat(value).isNotEqualTo(SecureSecretGenerator().generate(48))
  }

  @Test
  fun `rejects unsafe rotation requests`() {
    assertThrows<IllegalArgumentException> {
      SecretRotationRequest("db-password", "bitwarden://item/db/password")
    }
    assertThrows<IllegalArgumentException> {
      SecretRotationRequest("DB_PASSWORD", "", 32)
    }
    assertThrows<IllegalArgumentException> {
      SecretRotationRequest("DB_PASSWORD", "bitwarden://item/db/password", 8)
    }
  }

  @Test
  fun `parses explicit rotation modes`() {
    assertThat(SecretRotationMode.parse("dry-run")).isEqualTo(SecretRotationMode.DRY_RUN)
    assertThat(SecretRotationMode.parse("apply")).isEqualTo(SecretRotationMode.APPLY)
  }
}
