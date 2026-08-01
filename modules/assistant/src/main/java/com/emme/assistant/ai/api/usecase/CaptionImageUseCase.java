package com.emme.assistant.ai.api.usecase;

/** Captions an image through the configured AI capability. */
public interface CaptionImageUseCase {

  String caption(String imageBase64);
}
