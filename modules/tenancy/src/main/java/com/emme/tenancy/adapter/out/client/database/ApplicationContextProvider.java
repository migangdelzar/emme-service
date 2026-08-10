package com.emme.tenancy.adapter.out.client.database;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public final class ApplicationContextProvider
    implements ApplicationContextAware, PriorityOrdered {

  private static volatile ApplicationContext context;

  @Override
  public void setApplicationContext(@NonNull ApplicationContext ctx) {
    context = ctx;
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }

  public static ApplicationContext get() {
    return context;
  }
}
