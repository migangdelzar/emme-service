package com.emme.assistant.ai.api.result;

import java.util.Map;

public record IntentInfo(String intent, double confidence, Map<String, String> parameters) {}
