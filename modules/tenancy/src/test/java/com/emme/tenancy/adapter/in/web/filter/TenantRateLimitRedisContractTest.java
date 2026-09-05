package com.emme.tenancy.adapter.in.web.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.StringRedisTemplate;

class TenantRateLimitRedisContractTest {

  @Test
  void requiresTheProjectStandardStringRedisTemplate() {
    ConditionalOnBean condition =
        TenantRateLimitInterceptor.class.getAnnotation(ConditionalOnBean.class);

    assertThat(condition).isNotNull();
    assertThat(condition.value()).containsExactly(StringRedisTemplate.class);
  }

  @Test
  void receivesTheStringRedisTemplateAtItsAdapterBoundary() {
    Constructor<?> constructor = TenantRateLimitInterceptor.class.getConstructors()[0];

    assertThat(constructor.getParameterTypes()).contains(StringRedisTemplate.class);
  }
}
