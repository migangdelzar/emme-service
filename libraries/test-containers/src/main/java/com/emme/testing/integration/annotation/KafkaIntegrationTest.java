package com.emme.testing.integration.annotation;

import com.emme.testing.integration.container.KafkaContainerConfiguration;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/** Composed annotation for tests that exercise Kafka event streaming. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ActiveProfiles("kafka-test")
@Import(KafkaContainerConfiguration.class)
public @interface KafkaIntegrationTest {}
