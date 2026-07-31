package com.emme.buildlogic.internal

object TaskNames {
  const val CI = "ci"
  const val FULL = "full"
  const val CONTAINER_BUILD = "containerBuild"
  const val CONTAINER_PUSH = "containerPush"
  const val CONTAINER_VERIFY = "containerVerify"
  const val CONTAINER_MULTI_ARCH = "containerMultiArch"
  const val PUBLISH_INFO = "publishInfo"
  const val PUBLISH_MANIFEST = "publishManifest"
  const val PUBLISH_VERIFY = "publishVerify"
  const val PUBLISH_SIGN = "publishSign"
  const val PUBLISH_SBOM = "publishSbom"
  const val INTEGRATION_TEST = "integrationTest"
  const val ARCHITECTURE_TEST = "architectureTest"
}
