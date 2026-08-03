package com.emme.studio.documents.configuration;

import com.emme.studio.documents.adapter.out.persistence.mapper.DocumentPersistenceMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Spring wiring owned by the Documents capability. */
@Configuration
public class DocumentConfiguration {

  @Bean
  DocumentPersistenceMapper documentPersistenceMapper() {
    return new DocumentPersistenceMapper();
  }
}
