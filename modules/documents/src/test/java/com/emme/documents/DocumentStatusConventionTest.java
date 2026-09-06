package com.emme.documents;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.documents.adapter.in.web.response.DocumentResponse;
import com.emme.documents.api.result.DocumentDetails;
import com.emme.documents.api.type.DocumentStatus;
import org.junit.jupiter.api.Test;

class DocumentStatusConventionTest {

  @Test
  void documentStatusUsesAnApiOwnedEnumAcrossPublicBoundaries() {
    assertThat(DocumentDetails.class.getRecordComponents()[4].getType())
        .isEqualTo(DocumentStatus.class);
    assertThat(DocumentResponse.class.getRecordComponents()[4].getType())
        .isEqualTo(DocumentStatus.class);
  }
}
