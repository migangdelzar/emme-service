package com.emme.buildlogic.google

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.nio.file.Files

class GoogleOAuthCredentialsTest {
  @Test
  fun `loads web client credentials and redirect URIs`() {
    val file = Files.createTempFile("google-client", ".json").toFile()
    file.writeText(
      """
      {
        "web": {
          "client_id": "client-id.apps.googleusercontent.com",
          "client_secret": "secret",
          "redirect_uris": ["http://localhost/callback"]
        }
      }
      """.trimIndent(),
    )

    val credentials = GoogleOAuthCredentialsLoader.load(file)

    assertThat(credentials.clientId).isEqualTo("client-id.apps.googleusercontent.com")
    assertThat(credentials.redirectUris).containsExactly("http://localhost/callback")
    file.delete()
  }

  @Test
  fun `rejects a JSON file without a web client secret`() {
    val file = Files.createTempFile("google-client", ".json").toFile()
    file.writeText("{\"web\": {\"client_id\": \"client-id\"}}")

    assertThatThrownBy { GoogleOAuthCredentialsLoader.load(file) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessageContaining("client_secret")

    file.delete()
  }
}
