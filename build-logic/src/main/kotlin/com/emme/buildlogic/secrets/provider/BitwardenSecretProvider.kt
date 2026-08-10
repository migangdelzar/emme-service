package com.emme.buildlogic.secrets.provider

import com.emme.buildlogic.secrets.SecretProviderKind
import com.emme.buildlogic.secrets.generator.SecretGenerator
import com.emme.buildlogic.secrets.model.SecretRotationMode
import com.emme.buildlogic.secrets.model.SecretRotationReport
import com.emme.buildlogic.secrets.model.SecretRotationRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode

/** Validates an authenticated Bitwarden CLI session without retrieving values. */
class BitwardenSecretProvider(
  private val environment: Map<String, String> = System.getenv(),
  private val runner: SecretCommandRunner = SecretCommandRunner.system(environment),
  private val mapper: ObjectMapper = ObjectMapper(),
) : SecretProvider {
  override val kind = SecretProviderKind.BITWARDEN

  override fun validate(requiredNames: Set<String>): Set<String> {
    val authenticated =
      !environment["BW_SESSION"].isNullOrBlank() ||
        !environment["BWS_ACCESS_TOKEN"].isNullOrBlank()
    check(authenticated) {
      "Bitwarden provider requires BW_SESSION or BWS_ACCESS_TOKEN; secret values are not read by Gradle"
    }
    return emptySet()
  }

  override fun rotate(
    requests: List<SecretRotationRequest>,
    mode: SecretRotationMode,
    generator: SecretGenerator,
  ): SecretRotationReport {
    if (mode == SecretRotationMode.DRY_RUN) {
      return super.rotate(requests, mode, generator)
    }
    validate(emptySet())
    val entries =
      requests.map { request ->
        val reference = BitwardenReference.parse(request.reference)
        val item = runner.run(listOf("bw", "get", "item", reference.item), null).requireSuccess()
        val json =
          mapper.readTree(item.stdout) as? ObjectNode
            ?: error("Bitwarden returned an invalid item for ${request.name}")
        val generated = generator.generate(request.length).toString()
        update(json, reference, generated, request.name)
        val encoded =
          runner
            .run(listOf("bw", "encode"), json.toString())
            .requireSuccess()
            .stdout
            .trim()
        val itemId =
          json.path("id").asText().takeIf(String::isNotBlank)
            ?: error("Bitwarden item id is missing for ${request.name}")
        val command =
          buildList {
            addAll(listOf("bw", "edit", "item", itemId, encoded))
            json.path("organizationId").asText().takeIf(String::isNotBlank)?.let {
              addAll(listOf("--organizationid", it))
            }
          }
        runner.run(command, null).requireSuccess()
        SecretRotationReport.Entry(request.name, SecretRotationReport.Status.ROTATED)
      }
    return SecretRotationReport(kind.id, mode, entries)
  }

  private fun update(
    json: ObjectNode,
    reference: BitwardenReference,
    value: String,
    name: String,
  ) {
    when (reference.target) {
      "login.password" -> {
        json.withObject("login").put("password", value)
      }

      else -> {
        val fieldName = reference.target.removePrefix("field/")
        val field =
          json.withArray("fields").firstOrNull { it.path("name").asText() == fieldName }
            ?: error("Bitwarden field is missing for $name")
        (field as ObjectNode).put("value", value)
      }
    }
  }

  private data class BitwardenReference(
    val item: String,
    val target: String,
  ) {
    companion object {
      fun parse(value: String): BitwardenReference {
        val prefix = "bitwarden://item/"
        require(value.startsWith(prefix)) { "Bitwarden rotation reference must start with $prefix" }
        val remainder = value.removePrefix(prefix)
        val separator = remainder.indexOf('/')
        require(separator > 0 && separator < remainder.lastIndex) {
          "Bitwarden rotation reference must identify an item and target"
        }
        return BitwardenReference(remainder.substring(0, separator), remainder.substring(separator + 1))
      }
    }
  }
}
