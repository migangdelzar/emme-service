package com.emme.testing;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base class for L3 repository tests. Loads minimal Spring context — no web, no security, just JPA
 * + H2.
 *
 * <p>Usage: {@code class CustomerRepositoryTest extends BaseRepositoryTest} Inject: @Autowired
 * repositories directly
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
@Import(TestSecurityConfig.class)
@ActiveProfiles("repository")
public abstract class BaseRepositoryTest {
  // Subclasses inject @Autowired repositories
}
