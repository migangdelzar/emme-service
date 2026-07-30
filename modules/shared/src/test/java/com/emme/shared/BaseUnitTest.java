package com.emme.shared;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Base class for L1 unit tests. Pure Mockito — no Spring context, no database, no security. Usage:
 * {@code class MyTest extends BaseUnitTest}
 */
@ExtendWith(MockitoExtension.class)
public abstract class BaseUnitTest {
  // No shared state — each test is isolated
}
