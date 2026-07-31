package com.emme.buildlogic.provider.publishing

data class SignResult(
  val signaturePath: String,
)

data class PublishResult(
  val url: String,
  val digest: String,
)
