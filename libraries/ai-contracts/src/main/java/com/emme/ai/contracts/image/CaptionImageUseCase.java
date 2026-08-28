package com.emme.ai.contracts.image;

/** Captions an image through the configured AI capability. */
public interface CaptionImageUseCase {

  String caption(String imageBase64);
}
