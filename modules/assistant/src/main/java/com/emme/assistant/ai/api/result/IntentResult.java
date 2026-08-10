package com.emme.assistant.ai.api.result;

import java.util.Map;

public record IntentResult(String intent, double confidence, Map<String, String> parameters) {}
