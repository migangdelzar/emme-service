package com.emme.assistant.ai.application.port.out;

import java.util.Arrays;
import java.util.Optional;

/** Secure application boundary for reading tenant-authorized design images. */
public interface DesignImageReader {

  Optional<StoredImage> read(String storageKey);

  record StoredImage(byte[] bytes, String mediaType) {
    public StoredImage {
      if (bytes == null || bytes.length == 0) {
        throw new IllegalArgumentException("image bytes must not be empty");
      }
      if (mediaType == null || mediaType.isBlank()) {
        throw new IllegalArgumentException("mediaType must not be blank");
      }
      bytes = Arrays.copyOf(bytes, bytes.length);
    }

    @Override
    public byte[] bytes() {
      return Arrays.copyOf(bytes, bytes.length);
    }
  }
}
