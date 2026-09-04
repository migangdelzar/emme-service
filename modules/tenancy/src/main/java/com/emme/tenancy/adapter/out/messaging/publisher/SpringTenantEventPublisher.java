package com.emme.tenancy.adapter.out.messaging.publisher;

import com.emme.tenancy.api.event.TenantCreated;
import com.emme.tenancy.application.port.out.TenantEventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Publishes tenant lifecycle facts through Spring Modulith's event infrastructure. */
@Component
public class SpringTenantEventPublisher implements TenantEventPublisher {

  private final ApplicationEventPublisher publisher;

  public SpringTenantEventPublisher(ApplicationEventPublisher publisher) {
    this.publisher = publisher;
  }

  @Override
  public void publish(TenantCreated event) {
    publisher.publishEvent(event);
  }
}
