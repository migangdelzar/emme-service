package com.emme;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ConfigurationPropertiesScan
@ComponentScan(
    basePackages = "com.emme",
    excludeFilters = {
      @ComponentScan.Filter(
          type = FilterType.REGEX,
          pattern = "com\\.emme\\.tenancy\\.(config|pool|entity)\\..*"),
      @ComponentScan.Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class)
    })
@EnableJpaRepositories(basePackages = "com.emme")
public class TestApplication {}
