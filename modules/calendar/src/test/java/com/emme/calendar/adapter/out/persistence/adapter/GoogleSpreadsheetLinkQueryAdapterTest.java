package com.emme.calendar.adapter.out.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.calendar.adapter.out.persistence.mapper.GoogleSpreadsheetLinkMapper;
import com.emme.calendar.adapter.out.persistence.repository.SpringDataGoogleSpreadsheetLinkRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class GoogleSpreadsheetLinkQueryAdapterTest {

  @Test
  void listsSpreadsheetLinksFromTheCurrentTenantSchema() {
    SpringDataGoogleSpreadsheetLinkRepository repository = org.mockito.Mockito.mock();
    GoogleSpreadsheetLinkQueryAdapter adapter =
        new GoogleSpreadsheetLinkQueryAdapter(repository, new GoogleSpreadsheetLinkMapper());
    when(repository.findAll()).thenReturn(List.of());

    assertThat(adapter.findAll()).isEmpty();

    verify(repository).findAll();
  }
}
