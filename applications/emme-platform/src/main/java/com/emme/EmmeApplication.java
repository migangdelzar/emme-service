package com.emme;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class EmmeApplication {

  public static void main(String[] args) {
    SpringApplication.run(EmmeApplication.class, args);
  }
}
