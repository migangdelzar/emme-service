package com.emme.testing;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/** Base class for unit tests. No Spring context — Mockito only. */
@ExtendWith(MockitoExtension.class)
public abstract class BaseUnitTest {}
