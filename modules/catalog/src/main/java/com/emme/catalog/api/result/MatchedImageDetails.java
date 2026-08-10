package com.emme.catalog.api.result;

import java.util.UUID;

public record MatchedImageDetails(UUID imageId, String storageKey) {}
