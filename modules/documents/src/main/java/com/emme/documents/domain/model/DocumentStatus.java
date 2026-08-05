package com.emme.documents.domain.model;

/** Lifecycle states for a Studio document. */
public enum DocumentStatus {
  UPLOADED,
  PROCESSING,
  READY,
  FAILED,
  RETIRED
}
