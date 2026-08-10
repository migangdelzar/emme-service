package com.emme.documents.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;

/** HTTP request for uploading a document. */
public record UploadDocumentRequest(@NotBlank String name, @NotBlank String sourceType) {}
