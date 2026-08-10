package com.emme.buildlogic.secrets.manifest

import com.emme.buildlogic.secrets.model.SecretRotationRequest
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File

/** Reads provider-neutral secret references; the manifest never contains values. */
class SecretManifestLoader(
  private val mapper: ObjectMapper = ObjectMapper(),
) {
  fun load(
    file: File,
    environment: String,
  ): List<SecretRotationRequest> {
    if (!file.isFile) return emptyList()
    val root = mapper.readTree(file)
    require(root.path("version").asInt() == 1) { "Unsupported secret manifest version" }
    val environmentNode = root.path("environments").path(environment)
    require(!environmentNode.isMissingNode) { "Secret manifest has no environment '$environment'" }
    return environmentNode.path("secrets").map { node -> node.toRequest() }
  }

  private fun JsonNode.toRequest(): SecretRotationRequest =
    SecretRotationRequest(
      name = requiredText("name"),
      reference = requiredText("reference"),
      length = path("length").takeIf { !it.isMissingNode }?.asInt() ?: 32,
    )

  private fun JsonNode.requiredText(name: String): String =
    path(name).asText().takeIf(String::isNotBlank)
      ?: error("Secret manifest entry requires '$name'")
}
