package com.emme.catalog.api.result;

import java.util.UUID;

public record MatchedImageInfo(UUID imageId, String storageKey) {}
