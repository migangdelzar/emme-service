package com.emme.tenancy.adapter.out.client.database;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public final class ApplicationContextProvider implements ApplicationContextAware {

  private static volatile ApplicationContext context;

  @Override
  public void setApplicationContext(@NonNull ApplicationContext applicationContext)
      throws BeansException {
    context = applicationContext;
  }

  public static ApplicationContext get() {
    return context;
  }
}
