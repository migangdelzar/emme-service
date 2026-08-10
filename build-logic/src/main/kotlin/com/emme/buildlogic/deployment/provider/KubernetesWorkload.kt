package com.emme.buildlogic.deployment.provider

internal object KubernetesWorkload {
  const val DEPLOYMENT_NAME = "backend"
  const val POD_SELECTOR = "app=emme-backend"
}
