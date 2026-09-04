package com.emme.tenancy.application.port.out;

import com.emme.tenancy.api.event.TenantCreated;

/** Publishes tenant lifecycle facts without coupling application services to Spring events. */
public interface TenantEventPublisher {

  void publish(TenantCreated event);
}
