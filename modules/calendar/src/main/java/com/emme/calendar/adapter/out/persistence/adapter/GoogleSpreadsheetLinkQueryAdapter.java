package com.emme.calendar.adapter.out.persistence.adapter;

import com.emme.calendar.adapter.out.persistence.mapper.GoogleSpreadsheetLinkMapper;
import com.emme.calendar.adapter.out.persistence.repository.SpringDataGoogleSpreadsheetLinkRepository;
import com.emme.calendar.api.result.GoogleSpreadsheetDetails;
import com.emme.calendar.application.port.out.GoogleSpreadsheetLinkQueryPort;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GoogleSpreadsheetLinkQueryAdapter implements GoogleSpreadsheetLinkQueryPort {
  private final SpringDataGoogleSpreadsheetLinkRepository repository;
  private final GoogleSpreadsheetLinkMapper mapper;

  public GoogleSpreadsheetLinkQueryAdapter(
      SpringDataGoogleSpreadsheetLinkRepository repository, GoogleSpreadsheetLinkMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<GoogleSpreadsheetDetails> findAll() {
    return repository.findAll().stream().map(mapper::toResult).toList();
  }
}
