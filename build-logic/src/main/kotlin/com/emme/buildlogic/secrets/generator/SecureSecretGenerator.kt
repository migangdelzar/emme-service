package com.emme.buildlogic.secrets.generator

import java.security.SecureRandom

/** Cryptographically strong generator for opaque application secrets. */
class SecureSecretGenerator(
  private val random: SecureRandom = SecureRandom(),
) : SecretGenerator {
  override fun generate(length: Int): CharSequence {
    require(length > 0) { "Secret length must be positive" }
    return buildString(length) {
      repeat(length) {
        append(ALPHABET[random.nextInt(ALPHABET.length)])
      }
    }
  }

  private companion object {
    private const val ALPHABET =
      "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%^&*_-+="
  }
}
