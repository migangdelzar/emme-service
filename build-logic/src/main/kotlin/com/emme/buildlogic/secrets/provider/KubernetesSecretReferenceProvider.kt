package com.emme.buildlogic.secrets.provider

import com.emme.buildlogic.secrets.SecretProviderKind

/** Validates that Kubernetes references exist as declarations, not as values. */
class KubernetesSecretReferenceProvider : SecretProvider {
  override val kind = SecretProviderKind.KUBERNETES

  override fun validate(requiredNames: Set<String>): Set<String> = emptySet()
}
