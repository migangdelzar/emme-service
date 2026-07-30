package com.emme.assistant.ai.application;

import java.util.Map;

@FunctionalInterface
public interface ToolExecutor {
  String execute(Map<String, String> params) throws Exception;
}
