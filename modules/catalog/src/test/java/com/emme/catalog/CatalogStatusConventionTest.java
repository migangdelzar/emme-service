package com.emme.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.catalog.adapter.in.web.response.CatalogItemResponse;
import com.emme.catalog.api.result.CatalogItemDetails;
import com.emme.catalog.api.type.CatalogItemStatus;
import org.junit.jupiter.api.Test;

class CatalogStatusConventionTest {

  @Test
  void catalogStatusUsesAnApiOwnedEnumAcrossPublicBoundaries() {
    assertThat(CatalogItemDetails.class.getRecordComponents()[9].getType())
        .isEqualTo(CatalogItemStatus.class);
    assertThat(CatalogItemResponse.class.getRecordComponents()[9].getType())
        .isEqualTo(CatalogItemStatus.class);
  }
}
