package com.emme.ai.platform.adapter.out.provider.springai;

import com.emme.ai.contracts.image.CaptionImageUseCase;
import java.util.Base64;
import java.util.Objects;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.util.MimeTypeUtils;

/** Thin multimodal adapter over the existing Spring AI {@link ChatClient} transport. */
public final class SpringAiVisionModel implements CaptionImageUseCase {

  private static final String SYSTEM_PROMPT =
      "Describe the supplied image concisely and factually. "
          + "Do not infer prices, availability, permissions, or appointments.";
  private static final String DEFAULT_MEDIA_TYPE = "image/jpeg";

  private final ChatClient client;

  public SpringAiVisionModel(ChatClient client) {
    this.client = Objects.requireNonNull(client, "client must not be null");
  }

  public String caption(String imageBase64) {
    EncodedImage image = decode(imageBase64);
    String content =
        client
            .prompt()
            .system(SYSTEM_PROMPT)
            .user(
                user ->
                    user.text("Caption this image.")
                        .media(
                            Media.builder()
                                .mimeType(MimeTypeUtils.parseMimeType(image.mediaType()))
                                .data(image.bytes())
                                .build()))
            .call()
            .content();
    if (content == null || content.isBlank()) {
      throw new IllegalStateException("Vision provider returned an empty caption");
    }
    return content.strip();
  }

  private static EncodedImage decode(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("imageBase64 must not be blank");
    }
    String input = value.strip();
    String mediaType = DEFAULT_MEDIA_TYPE;
    String payload = input;
    if (input.startsWith("data:")) {
      int separator = input.indexOf(',');
      if (separator <= "data:".length()) {
        throw new IllegalArgumentException("imageBase64 data URI is invalid");
      }
      String metadata = input.substring("data:".length(), separator);
      if (!metadata.endsWith(";base64")) {
        throw new IllegalArgumentException("imageBase64 data URI must be base64 encoded");
      }
      mediaType = metadata.substring(0, metadata.length() - ";base64".length());
      payload = input.substring(separator + 1);
    }
    try {
      byte[] bytes = Base64.getDecoder().decode(payload);
      if (bytes.length == 0) throw new IllegalArgumentException("imageBase64 must not be empty");
      return new EncodedImage(mediaType, bytes);
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException("imageBase64 must be valid base64", exception);
    }
  }

  private record EncodedImage(String mediaType, byte[] bytes) {}
}
