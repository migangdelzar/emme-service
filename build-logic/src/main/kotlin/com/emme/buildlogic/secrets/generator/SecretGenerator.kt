package com.emme.buildlogic.secrets.generator

/** Generates replacement values without exposing them to Gradle models or logs. */
fun interface SecretGenerator {
  fun generate(length: Int): CharSequence
}
