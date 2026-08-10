package com.emme.buildlogic.publishing.provider

data class SignResult(
  val signaturePath: String,
)

data class PublishResult(
  val url: String,
  val digest: String,
)
