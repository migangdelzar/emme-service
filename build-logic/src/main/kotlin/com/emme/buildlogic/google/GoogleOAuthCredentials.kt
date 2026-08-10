package com.emme.buildlogic.google

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File

data class GoogleOAuthCredentials(
  val clientId: String,
  val clientSecret: String,
  val redirectUris: List<String>,
)

object GoogleOAuthCredentialsLoader {
  private val mapper = ObjectMapper()

  fun load(file: File): GoogleOAuthCredentials {
    require(file.isFile) { "Google OAuth credentials file does not exist: ${file.absolutePath}" }
    val web = mapper.readTree(file).path("web")
    require(!web.isMissingNode) { "Google OAuth credentials must contain a web client" }

    val clientId = web.requiredText("client_id")
    val clientSecret = web.requiredText("client_secret")
    val redirectUris = web.path("redirect_uris").map { it.asText() }
    return GoogleOAuthCredentials(clientId, clientSecret, redirectUris)
  }

  private fun com.fasterxml.jackson.databind.JsonNode.requiredText(name: String): String {
    val value = path(name).asText("")
    require(value.isNotBlank()) { "Google OAuth credentials are missing web.$name" }
    return value
  }
}

object GoogleOAuthCredentialsFileResolver {
  fun resolve(
    explicitPath: String?,
    home: File,
    predicate: (GoogleOAuthCredentials) -> Boolean,
  ): File {
    if (!explicitPath.isNullOrBlank()) {
      return File(explicitPath).also {
        require(it.isFile) { "Google OAuth credentials file does not exist: ${it.absolutePath}" }
      }
    }

    val candidates =
      home
        .resolve("Downloads")
        .listFiles { file -> file.isFile && file.name.startsWith("client_secret_") && file.extension == "json" }
        .orEmpty()
        .filter { file -> runCatching { predicate(GoogleOAuthCredentialsLoader.load(file)) }.getOrDefault(false) }

    require(candidates.size == 1) {
      "Expected exactly one matching Google OAuth JSON in ${home.resolve("Downloads")}; " +
        "set the corresponding Gradle property explicitly"
    }
    return candidates.single()
  }
}
