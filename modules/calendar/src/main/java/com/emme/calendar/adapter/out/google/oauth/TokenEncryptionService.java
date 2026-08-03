package com.emme.calendar.adapter.out.google.oauth;

import com.emme.calendar.configuration.GoogleOAuthProperties;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

/**
 * AES-256-GCM encryption service for Google OAuth token storage.
 *
 * <p>Uses a 32-byte key from {@link GoogleOAuthProperties#encryptionKey()}. Each encryption produces a
 * random 12-byte IV, prepended to the ciphertext. The result is Base64-encoded.
 *
 * <p>Algorithm: {@code AES/GCM/NoPadding}, 128-bit authentication tag.
 */
@Service
public class TokenEncryptionService {

  private static final String ALGORITHM = "AES/GCM/NoPadding";
  private static final int GCM_TAG_LENGTH = 128;
  private static final int GCM_IV_LENGTH = 12;

  private final byte[] key;

  /**
   * Creates the service, validating the encryption key is exactly 32 bytes for AES-256.
   *
   * @param properties OAuth properties holding the raw key string
   * @throws IllegalArgumentException if the key is not 32 bytes
   */
  public TokenEncryptionService(GoogleOAuthProperties properties) {
    byte[] rawKey = properties.encryptionKey().getBytes(StandardCharsets.UTF_8);
    if (rawKey.length != 32) {
      throw new IllegalArgumentException(
          "encryption-key must be exactly 32 bytes for AES-256, got " + rawKey.length);
    }
    this.key = rawKey;
  }

  /**
   * Encrypts plaintext using AES-256-GCM.
   *
   * @param plaintext the data to encrypt
   * @return Base64-encoded ciphertext with 12-byte IV prepended
   */
  public String encrypt(String plaintext) {
    try {
      byte[] iv = new byte[GCM_IV_LENGTH];
      SecureRandom.getInstanceStrong().nextBytes(iv);

      Cipher cipher = Cipher.getInstance(ALGORITHM);
      GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
      cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), spec);

      byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
      byte[] combined = new byte[GCM_IV_LENGTH + ciphertext.length];
      System.arraycopy(iv, 0, combined, 0, GCM_IV_LENGTH);
      System.arraycopy(ciphertext, 0, combined, GCM_IV_LENGTH, ciphertext.length);

      return Base64.getEncoder().encodeToString(combined);
    } catch (Exception e) {
      throw new RuntimeException("Failed to encrypt token", e);
    }
  }

  /**
   * Decrypts a Base64-encoded IV+ciphertext produced by {@link #encrypt(String)}.
   *
   * @param encrypted the Base64-encoded payload
   * @return the original plaintext
   */
  public String decrypt(String encrypted) {
    try {
      byte[] combined = Base64.getDecoder().decode(encrypted);
      byte[] iv = new byte[GCM_IV_LENGTH];
      byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH];
      System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
      System.arraycopy(combined, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

      Cipher cipher = Cipher.getInstance(ALGORITHM);
      GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
      cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), spec);

      return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new RuntimeException("Failed to decrypt token", e);
    }
  }
}
