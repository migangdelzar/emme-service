package com.emme.assistant.ai.adapter.out.provider.springai;

import com.emme.assistant.ai.application.port.out.DesignImageReader;
import com.emme.assistant.ai.application.port.out.NailDesignExtractionRejectedException;
import com.emme.assistant.ai.application.port.out.NailDesignExtractor;
import com.emme.assistant.ai.domain.quote.NailDesignFeatures;
import java.util.Objects;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.util.MimeTypeUtils;

/** Spring AI structured-output adapter for text and securely loaded design images. */
public final class SpringAiNailDesignExtractor implements NailDesignExtractor {

  private static final String SYSTEM_PROMPT =
      "You extract nail-design attributes for Emme. Return only the requested structured schema. "
          + "Use null when a field is not visible or cannot be determined. "
          + "Use confidenceByField values from 0 to 1 and list every ambiguity. "
          + "Never calculate prices, availability, or appointments.";

  private final ChatClient chatClient;
  private final String modelVersion;
  private final String promptVersion;
  private final String schemaVersion;
  private final DesignImageReader imageReader;

  public SpringAiNailDesignExtractor(
      ChatClient chatClient,
      String modelVersion,
      String promptVersion,
      String schemaVersion,
      DesignImageReader imageReader) {
    this.chatClient = Objects.requireNonNull(chatClient, "chatClient must not be null");
    this.modelVersion = requireText(modelVersion, "modelVersion");
    this.promptVersion = requireText(promptVersion, "promptVersion");
    this.schemaVersion = requireText(schemaVersion, "schemaVersion");
    this.imageReader = Objects.requireNonNull(imageReader, "imageReader must not be null");
  }

  @Override
  public ExtractionResult extract(ExtractionRequest request) {
    Objects.requireNonNull(request, "request must not be null");
    try {
      NailDesignFeatures features =
          request.imageStorageKey() == null ? extractText(request) : extractImage(request);
      if (features == null) {
        throw new NailDesignExtractionRejectedException(
            "Spring AI returned no nail-design features");
      }
      return new ExtractionResult(features, modelVersion, promptVersion, schemaVersion);
    } catch (NailDesignExtractionRejectedException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      throw new NailDesignExtractionRejectedException(
          "Spring AI nail-design extraction failed", exception);
    }
  }

  private NailDesignFeatures extractText(ExtractionRequest request) {
    return structuredEntity(
        chatClient.prompt().system(SYSTEM_PROMPT).user(request.inputText()).call());
  }

  private NailDesignFeatures extractImage(ExtractionRequest request) {
    DesignImageReader.StoredImage image =
        imageReader
            .read(request.imageStorageKey())
            .orElseThrow(
                () ->
                    new NailDesignExtractionRejectedException(
                        "Design image is unavailable: " + request.imageStorageKey()));
    String userText =
        request.inputText() == null
            ? "Analyze the attached nail-design image."
            : request.inputText();
    return structuredEntity(
        chatClient
            .prompt()
            .system(SYSTEM_PROMPT)
            .user(
                user ->
                    user.text(userText)
                        .media(
                            Media.builder()
                                .mimeType(MimeTypeUtils.parseMimeType(image.mediaType()))
                                .data(image.bytes())
                                .build()))
            .call());
  }

  private NailDesignFeatures structuredEntity(ChatClient.CallResponseSpec response) {
    return response.entity(
        NailDesignFeatures.class,
        options -> options.validateSchema().useProviderStructuredOutput());
  }

  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value;
  }
}
