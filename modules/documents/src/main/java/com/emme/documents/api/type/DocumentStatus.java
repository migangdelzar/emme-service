package com.emme.documents.api.type;

/** Stable document lifecycle values exposed by the application API. */
public enum DocumentStatus {
  UPLOADED,
  PROCESSING,
  READY,
  FAILED,
  RETIRED
}
